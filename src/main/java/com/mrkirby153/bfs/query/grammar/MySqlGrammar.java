package com.mrkirby153.bfs.query.grammar;

import com.mrkirby153.bfs.query.QueryBuilder;
import com.mrkirby153.bfs.query.elements.WhereElement;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
public class MySqlGrammar implements Grammar {

    private final Pattern columnPattern = Pattern.compile("(.*)\\.(.*)");
    private final Pattern preWrapped = Pattern.compile("`.*`");
    /**
     * An ordered list of components and the order they are compiled in
     */
    private String[] components = new String[]{
        "columns",
        "from",
        "joins",
        "wheres",
        "groups",
        "havings",
        "orders",
        "limit",
        "offset",
        "unions",
        "lock"
    };

    @Override
    public String compileSelect(QueryBuilder builder) {
        return compileSelectComponents(builder);
    }

    @Override
    public String compileUpdate(QueryBuilder builder, String... columnNames) {
        String columns = Arrays.stream(columnNames).map(this::wrapColumn)
            .collect(Collectors.joining(", "));
        return String.format("UPDATE %s SET %s %s", wrap(builder.getTable()), columns,
            compileWheres(builder));
    }

    @Override
    public String compileDelete(QueryBuilder builder) {
        return String.format("DELETE FROM %s %s", wrap(builder.getTable()), compileWheres(builder));
    }

    @Override
    public String compileExists(QueryBuilder builder) {
        return String.format("SELECT EXISTS(%s) AS `exists`", compileSelect(builder));
    }

    @Override
    public String compileInsert(QueryBuilder builder, String... columnNames) {
        return String.format("INSERT INTO %s (%s) VALUES (%s)", wrap(builder.getTable()),
            Arrays.stream(columnNames).map(this::wrapColumn).collect(Collectors.joining(", ")),
            Arrays.stream(columnNames).map(this::parameter).collect(Collectors.joining(", ")));
    }

    @Override
    public String compileInsertMany(QueryBuilder builder, long count, String... columnNames) {
        String params = Arrays.stream(columnNames).map(this::parameter)
            .collect(Collectors.joining(", "));
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < count; i++) {
            sb.append(String.format("(%s)", params));
            if (i + 1 < count) {
                sb.append(", ");
            }
        }
        return String.format("INSERT INTO %s (%s) VALUES %s", wrap(builder.getTable()),
            Arrays.stream(columnNames).map(this::wrapColumn).collect(Collectors.joining(", ")),
            sb.toString());
    }

    @Override
    public void bind(QueryBuilder builder, PreparedStatement statement) {
        AtomicInteger pos = new AtomicInteger(1);
        builder.getBindings().entrySet().stream().flatMap(entry -> entry.getValue().stream())
            .forEach(e -> {
                try {
                    statement.setObject(pos.getAndIncrement(), e);
                } catch (SQLException ex) {
                    log.error("Could not bind {} in query {}", e, statement, ex);
                }
            });
    }

    private String compileSelectComponents(QueryBuilder builder) {
        StringBuilder query = new StringBuilder();
        for (String s : this.components) {
            String methodName = "compile" + uppercaseFirst(s);
            log.debug("Compiling {} ({})", s, methodName);
            try {
                Method m = this.getClass().getDeclaredMethod(methodName, QueryBuilder.class);
                String result = (String) m.invoke(this, builder);
                if (!result.isEmpty()) {
                    query.append(result);
                    query.append(" ");
                }
            } catch (NoSuchMethodException e) {
                log.warn("Method {} does not exist", methodName);
            } catch (IllegalAccessException | InvocationTargetException e) {
                log.warn("An error occurred when compiling {}", s, e);
            }
        }
        return query.toString();
    }

    private String compileColumns(QueryBuilder builder) {
        StringBuilder s = new StringBuilder();
        s.append("SELECT ");
        if (builder.isDistinct()) {
            s.append("DISTINCT ");
        }
        if (builder.getColumns().isEmpty()) {
            s.append("*");
        } else {
            s.append(builder.getColumns().stream().map(this::wrapColumn)
                .collect(Collectors.joining(", ")));
        }
        return s.toString().trim();
    }

    private String compileFrom(QueryBuilder builder) {
        return String.format("FROM %s", wrap(builder.getTable()));
    }

    private String compileWheres(QueryBuilder builder) {
        if (builder.getWheres().isEmpty()) {
            return "";
        }
        return String.format("WHERE %s", appendWheres(builder.getWheres()));
    }

    private String compileJoins(QueryBuilder builder) {
        return builder.getJoins().stream().map(join -> {
            String joinType = "";
            switch (join.getType()) {
                case INNER:
                    joinType = "INNER";
                    break;
                case OUTER:
                    joinType = "FULL OUTER";
                    break;
                case LEFT:
                    joinType = "LEFT";
                    break;
                case RIGHT:
                    joinType = "RIGHT";
                    break;
            }
            return String.format("%s JOIN %s ON %s %s %s", joinType, wrap(join.getTable()),
                wrapColumn(join.getFirstColumn()), join.getOperation(),
                wrapColumn(join.getSecondColumn()));
        }).collect(Collectors.joining(" "));
    }

    private String compileLimit(QueryBuilder builder) {
        return builder.getLimit() != null ? String.format("LIMIT %d", builder.getLimit()) : "";
    }

    private String compileOffset(QueryBuilder builder) {
        return builder.getOffset() != null ? String.format("OFFSET %d", builder.getOffset()) : "";
    }

    private String compileOrders(QueryBuilder builder) {
        if (builder.getOrders().size() > 0) {

            return String.format("ORDER BY %s", builder.getOrders().stream().map(order -> {
                String direction = "";
                switch (order.getDirection()) {
                    case ASC:
                        direction = "ASC";
                        break;
                    case DESC:
                        direction = "DESC";
                        break;
                }
                return String.format("%s %s", wrapColumn(order.getColumn()), direction);
            }).collect(Collectors.joining(", ")));
        } else {
            return "";
        }
    }


    private String appendWheres(List<WhereElement> wheres) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < wheres.size(); i++) {
            WhereElement where = wheres.get(i);
            if (i > 0) {
                sb.append(where.getBool()).append(' ');
            }
            switch (where.getType()) {
                case BASIC:
                    sb.append(whereBasic(where));
                    break;
                case NOT_NULL:
                    sb.append(whereNull(where, true));
                    break;
                case NULL:
                    sb.append(whereNull(where, false));
                    break;
                case IN:
                    sb.append(whereIn(where, false));
                    break;
                case NOT_IN:
                    sb.append(whereIn(where, true));
                    break;
                case SUB:
                    sb.append(whereSub(where, false));
                    break;
                case NOT_SUB:
                    sb.append(whereSub(where, true));
                    break;
            }

            sb.append(" ");
        }
        return sb.toString().trim();
    }

    private String whereBasic(WhereElement e) {
        return String
            .format("%s %s %s", wrapColumn(e.getColumn()), e.get("operator"), parameter(e.get("value")));
    }

    private String whereNull(WhereElement e, boolean not) {
        return String.format(not ? "%s IS NOT NULL" : "%s IS NULL", wrapColumn(e.getColumn()));
    }

    private String whereIn(WhereElement e, boolean not) {
        return String.format(not ? "%s NOT IN (%s)" : "%s IN (%s)", wrapColumn(e.getColumn()),
            parameterize((Object[]) e.get("values")));
    }

    private String whereSub(WhereElement e, boolean not) {
        QueryBuilder subQuery = (QueryBuilder) e.get("query");
        return String.format(not ? "%s NOT IN (%s)" : "%s IN (%s)", e.getColumn(),
            subQuery.getGrammar().compileSelect(subQuery));
    }

    private String uppercaseFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private String wrap(String s) {
        // Special case * and don't wrap it
        if (s.equals("*")) {
            return s;
        }
        if (preWrapped.matcher(s).find()) {
            return s;
        }
        return String.format("`%s`", s);
    }

    private String wrapColumn(String s) {
        if (preWrapped.matcher(s).find()) {
            return s;
        }
        Matcher matcher = columnPattern.matcher(s);
        if (matcher.find()) {
            String table = wrap(matcher.group(1));
            String column = wrap(matcher.group(2));
            return String.format("%s.%s", table, column);
        } else {
            return wrap(s);
        }
    }

    private String parameter(Object value) {
        return "?";
    }

    private String parameterize(Object[] value) {
        return Arrays.stream(value).map(this::parameter).collect(Collectors.joining(", "));
    }
}
