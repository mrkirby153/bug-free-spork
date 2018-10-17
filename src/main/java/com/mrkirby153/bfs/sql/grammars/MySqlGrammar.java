package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.Pair;
import com.mrkirby153.bfs.sql.elements.WhereElement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class MySqlGrammar implements Grammar {

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
        return compileComponents(builder);
    }

    @Override
    public String compileUpdate(QueryBuilder builder, Pair... pairs) {
        String table = wrap(builder.getTable());

        StringBuilder columnBuilder = new StringBuilder();

        for (Pair p : pairs) {
            columnBuilder.append(wrapColumn(p.getColumn())).append(" = ")
                .append(parameter(p.getValue()))
                .append(", ");
        }

        String s = columnBuilder.toString();
        return "UPDATE " + table + " SET " + s.substring(0, s.length() - 2) + " " + this
            .compileWheres(builder);
    }


    @Override
    public String compileDelete(QueryBuilder builder) {
        String table = wrap(builder.getTable());
        return "DELETE FROM " + table + " " + this.compileWheres(builder);
    }

    @Override
    public String compileExists(QueryBuilder builder) {
        return "SELECT EXISTS(" + compileSelect(builder) + ") as `exists`";
    }

    @Override
    public String compileInsert(QueryBuilder builder, Pair... data) {
        StringBuilder cols = new StringBuilder();
        for (int i = 0; i < data.length; i++) {
            cols.append(wrapColumn(data[i].getColumn()));
            if (i + 1 < data.length) {
                cols.append(", ");
            }
        }

        return "INSERT INTO `" + builder.getTable() + "`(" + cols + ") VALUES (" + parameterize(
            data)
            + ")";
    }

    @Override
    public String compileInsertMany(QueryBuilder builder, List<List<Pair>> data) {
        StringBuilder cols = new StringBuilder();
        List<Pair> firstRow = data.get(0);
        for (int i = 0; i < firstRow.size(); i++) {
            cols.append(wrapColumn(firstRow.get(i).getColumn()));
            if (i + 1 < firstRow.size()) {
                cols.append(", ");
            }
        }
        StringBuilder values = new StringBuilder();
        for (int i = 0; i < data.size(); i++) {
            values.append("(");
            values.append(parameterize(
                data.get(i).stream().map(Pair::getValue).toArray(Object[]::new)));
            values.append(")");
            if (i + 1 < data.size()) {
                values.append(", ");
            }
        }
        return "INSERT INTO `" + builder.getTable() + "` (" + cols + ") VALUES " + values;
    }

    @Override
    public void bind(QueryBuilder builder, PreparedStatement statement) {
        int start = 1;
        for (Object o : builder.getBindings()) {
            try {
                statement.setObject(start++, o);
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * Compile the various components in a <code>SELECT</code> statement
     *
     * @param builder The builder to use
     *
     * @return The compiled sql
     */
    private String compileComponents(QueryBuilder builder) {
        StringBuilder query = new StringBuilder();
        for (String s : this.components) {
            String methodName = "compile" + uppercaseFirst(s);
            try {
                Method m = this.getClass().getDeclaredMethod(methodName, QueryBuilder.class);
                String result = (String) m.invoke(this, builder);
                if (!result.isEmpty()) {
                    query.append(result);
                    query.append(" ");
                }
            } catch (NoSuchMethodException e) {
                // Ignore
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return query.toString().trim();
    }

    private String compileColumns(QueryBuilder builder) {
        StringBuilder s = new StringBuilder();
        s.append("SELECT ");
        if (builder.isDistinct()) {
            s.append("DISTINCT ");
        }
        if (builder.getColumns().length == 0) {
            s.append("*");
        } else {
            for (int i = 0; i < builder.getColumns().length; i++) {
                String col = builder.getColumns()[i];

                s.append(wrapColumn(col));
                if (i + 1 < builder.getColumns().length) {
                    s.append(", ");
                }
            }
        }
        return s.toString().trim();
    }

    private String compileFrom(QueryBuilder builder) {
        return "FROM " + wrap(builder.getTable());
    }

    private String compileWheres(QueryBuilder builder) {
        if (builder.getWheres().isEmpty()) {
            return "";
        }
        return "WHERE " + appendWheres(builder.getWheres());
    }

    private String compileOrders(QueryBuilder builder) {
        StringBuilder clause = new StringBuilder();
        if (builder.getOrders().size() > 0) {
            builder.getOrders().forEach(e -> {
                clause.append(wrapColumn(e.getColumn())).append(" ").append(e.getDirection())
                    .append(", ");
            });
            String orderClause = clause.toString();
            return "ORDER BY " + orderClause.substring(0, orderClause.length() - 2);
        } else {
            return "";
        }
    }

    private String compileJoins(QueryBuilder builder) {
        StringBuilder joins = new StringBuilder();
        builder.getJoins().forEach(join -> {
            joins.append(join.getType().toString()).append(" JOIN ").append(join.getTable())
                .append(" ON ");
            joins.append(join.getFirst()).append(" ").append(join.getOperation()).append(" ")
                .append(join.getSecond());
        });
        return joins.toString().trim();
    }

    private String compileLimit(QueryBuilder builder) {
        if (builder.getLimit() != null) {
            return "LIMIT " + builder.getLimit();
        } else {
            return "";
        }
    }

    private String compileOffset(QueryBuilder builder) {
        if (builder.getOffset() != null) {
            return "OFFSET " + builder.getOffset();
        } else {
            return "";
        }
    }

    /**
     * Joins all the {@link WhereElement}s to string
     *
     * @param e The elements
     *
     * @return A string
     */
    private String appendWheres(ArrayList<WhereElement> e) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < e.size(); i++) {
            WhereElement g = e.get(i);
            if (i > 0) {
                Object operator = g.get("boolean");
                s.append(operator != null ? operator : "AND");
                s.append(" ");
            }
            String type = g.getType();
            try {
                Method m = this.getClass().getDeclaredMethod("where" + type, WhereElement.class);
                String result = m.invoke(this, g).toString();
                s.append(result);
            } catch (NoSuchMethodException e1) {
                System.out.println("No where method for type " + type);
            } catch (IllegalAccessException | InvocationTargetException e1) {
                e1.printStackTrace();
            }
            s.append(" ");
        }
        return s.toString();
    }

    private String whereBasic(WhereElement e) {
        return wrapColumn(e.get("column").toString()) + " " + e.get("operator") + " " + parameter(
            e.get("value"));
    }

    private String whereNull(WhereElement e) {
        return wrapColumn(e.get("column").toString()) + " IS NULL";
    }

    private String whereNotNull(WhereElement e) {
        return wrapColumn(e.get("column").toString()) + " IS NOT NULL";
    }

    private String whereIn(WhereElement e) {
        return wrapColumn(e.get("column").toString()) + " IN (" + parameterize(
            (Object[]) e.get("values"))
            + ")";
    }

    private String whereNotIn(WhereElement e) {
        return wrapColumn(e.get("column").toString()) + " NOT IN (" + parameterize(
            (Object[]) e.get("values")) + ")";
    }

    private String whereSub(WhereElement e) {
        return wrapColumn(
            e.get("column").toString()) + " IN (" + ((QueryBuilder) e.get("query")).getGrammar()
            .compileSelect(
                (QueryBuilder) e.get("query")) + ")";
    }

    private String whereNotSub(WhereElement e) {
        return wrapColumn(
            e.get("column").toString()) + " NOT IN (" + ((QueryBuilder) e.get("query")).getGrammar()
            .compileSelect(
                (QueryBuilder) e.get("query")) + ")";
    }


    /**
     * Capitalizes the first char in a string
     *
     * @param s The string
     *
     * @return The string with the first letter capitalized
     */
    private String uppercaseFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /**
     * Surrounds a string with `
     *
     * @param s The string
     *
     * @return The string wrapped with `
     */
    private String wrap(String s) {
        if (preWrapped.matcher(s).find()) {
            return s;
        }
        return "`" + s + "`";
    }

    private String parameterize(Object[] values) {
        return String
            .join(", ", Arrays.stream(values).map(this::parameter).collect(Collectors.toList()));
    }

    private String parameter(Object value) {
        return "?";
    }

    private final Pattern columnPattern = Pattern.compile("(.*)\\.(.*)");
    private final Pattern preWrapped = Pattern.compile("`.*`");

    private String wrapColumn(String name) {
        if (preWrapped.matcher(name).find()) {
            return name; // The column is wrapped already, ignore.
        }
        Matcher matcher = columnPattern.matcher(name);
        if (matcher.find()) {
            String table = "`" + matcher.group(1) + "`";
            String column =
                matcher.group(2).equalsIgnoreCase("*") ? "*" : "`" + matcher.group(2) + "`";
            return table + "." + column;
        } else {
            return "`" + name + "`";
        }
    }
}
