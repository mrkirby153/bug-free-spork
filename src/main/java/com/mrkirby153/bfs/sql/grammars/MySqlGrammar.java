package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.GenericElement;
import com.mrkirby153.bfs.sql.elements.Pair;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

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
    public void bindSelect(QueryBuilder builder, PreparedStatement statement) {
        bindWheres(builder, statement, 1);
    }

    @Override
    public String compileUpdate(QueryBuilder builder, Pair... pairs) {
        String table = "`" + builder.getTable() + "` ";

        StringBuilder columnBuilder = new StringBuilder();

        for (Pair p : pairs) {
            columnBuilder.append("`").append(p.getColumn()).append("` = ? ");
        }

        return "UPDATE " + table + " SET " + columnBuilder.toString() + this.compileWheres(builder);
    }

    @Override
    public void bindUpdate(QueryBuilder builder, PreparedStatement statement, Pair... pairs) {
        int index = 1;
        for (Pair p : pairs) {
            try {
                statement.setObject(index++, p.getValue());
            } catch (SQLException e) {
                e.printStackTrace();
            }
        }

        index = bindWheres(builder, statement, index);
    }

    private String compileComponents(QueryBuilder builder) {
        StringBuilder query = new StringBuilder();
        for (String s : this.components) {
            String methodName = "compile" + uppercaseFirst(s);
            try {
                Method m = this.getClass().getDeclaredMethod(methodName, QueryBuilder.class);
                query.append(m.invoke(this, builder));
                query.append(" ");
            } catch (NoSuchMethodException e) {
                // Ignore
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        }
        return query.toString();
    }

    private String compileColumns(QueryBuilder builder) {
        StringBuilder s = new StringBuilder();
        s.append("SELECT ");
        if (builder.getColumns().length == 0) {
            s.append("*");
        } else {
            for (int i = 0; i < builder.getColumns().length; i++) {
                s.append("'");
                s.append(builder.getColumns()[i]);
                s.append("'");
                if (i + 1 < builder.getColumns().length) {
                    s.append(", ");
                }
            }
        }
        return s.toString();
    }

    private String compileFrom(QueryBuilder builder) {
        return "from `" + builder.getTable() + "`";
    }

    private String compileWheres(QueryBuilder builder) {
        return "WHERE " + appendWheres(builder.getWheres()).replaceFirst("AND\\s?", "");
    }

    private String compileOrders(QueryBuilder builder) {
        StringBuilder clause = new StringBuilder();
        if (builder.getOrders().size() > 0) {
            builder.getOrders().forEach(e -> {
                clause.append("`").append(e.getColumn()).append("` ").append(e.getDirection())
                    .append(", ");
            });
            String orderClause = clause.toString();
            return "ORDER BY " + orderClause.substring(0, orderClause.length() - 2);
        } else {
            return "";
        }
    }

    private String appendWheres(ArrayList<GenericElement> e) {
        StringBuilder s = new StringBuilder();
        for (GenericElement g : e) {
            s.append("AND ");
            s.append(g.getQuery());
        }
        return s.toString();
    }

    private int bindWheres(QueryBuilder builder, PreparedStatement statement, int startIndex) {
        AtomicInteger a = new AtomicInteger(startIndex);
        builder.getWheres().forEach(w -> {
            w.getBindings().forEach(o -> {
                try {
                    statement.setObject(a.getAndIncrement(), o);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
        });
        return a.get();
    }


    private String uppercaseFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
