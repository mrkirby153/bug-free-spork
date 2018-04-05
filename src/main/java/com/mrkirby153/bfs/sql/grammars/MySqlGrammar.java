package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.GenericElement;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.PreparedStatement;
import java.util.ArrayList;

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
    public void bindSelect(PreparedStatement statement) {

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
            s.append("(");
            for (int i = 0; i < builder.getColumns().length; i++) {
                s.append(builder.getColumns()[i]);
                if (i + 1 < builder.getColumns().length) {
                    s.append(", ");
                }
            }
            s.append(")");
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
        builder.getOrders().forEach(e -> {
            clause.append("`").append(e.getColumn()).append("` ").append(e.getDirection())
                .append(", ");
        });
        String orderClause = clause.toString();
        return "ORDER BY " + orderClause.substring(0, orderClause.length() - 2);
    }

    private String appendWheres(ArrayList<GenericElement> e) {
        StringBuilder s = new StringBuilder();
        for (GenericElement g : e) {
            s.append("AND ");
            s.append(g.getQuery());
        }
        return s.toString();
    }


    private String uppercaseFirst(String s) {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }
}
