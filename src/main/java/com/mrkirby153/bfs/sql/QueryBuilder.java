package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.sql.elements.GenericElement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

/**
 * A query builder for translating model operations into SQL queries
 */
public class QueryBuilder<T extends Model> {

    private Class<T> modelClass;

    private T modelInstance;

    private HashMap<Binding, ArrayList<QueryElement>> queryBindings = new HashMap<>();

    public QueryBuilder(Class<T> clazz) {
        this.modelClass = clazz;
        try {
            this.modelInstance = clazz.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public QueryBuilder(T instance) {
        this.modelInstance = instance;
        this.modelClass = (Class<T>) instance.getClass();
    }

    /**
     * Adds a <code>WHERE</code> scope to the query
     *
     * @param column The column
     * @param object The value to compare
     *
     * @return The query builder
     */
    public QueryBuilder where(String column, Object object) {
        return this.where(column, "=", object);
    }

    /**
     * Adds a <code>WHERE</code> scope to the query
     *
     * @param column The column
     * @param test   The logical test
     * @param object The object to compare
     *
     * @return The query builder
     */
    public QueryBuilder where(String column, String test, Object object) {
        addBinding(Binding.WHERE, new GenericElement(String.format("%s %s ?", column, test)));
        return this;
    }

    /**
     * Sets the table to use in this query
     *
     * @param table The table to use
     *
     * @return The builder
     */
    public QueryBuilder table(String table) {
        addBinding(Binding.FROM, new GenericElement(String.format("`%s`", table)));
        return this;
    }

    /**
     * Constructs the query
     *
     * @return The query
     */
    public String buildQuery() {
        StringBuilder sb = new StringBuilder();
        for (Binding b : Binding.values()) {
            ArrayList<QueryElement> el = this.queryBindings.get(b);
            StringBuilder s = new StringBuilder();
            if (el == null) {
                continue;
            }
            el.forEach(e -> {
                s.append(b.getPrefix());
                s.append(" ");
                s.append(e.getQuery());
                s.append(" ");
            });
            String builtString = s.toString();

            // Remove the first boolean operator
            if (b == Binding.WHERE) {
                builtString = builtString.replaceFirst("AND\\s?", "");
            }
            sb.append(builtString);
        }
        return sb.toString();
    }

    /**
     * Selects the columns to be used
     *
     * @param columns The columns to use
     *
     * @return The query builder
     */
    public QueryBuilder columns(String... columns) {
        // Join the columns
        StringBuilder joinedColsBuilder = new StringBuilder();
        for (String s : columns) {
            joinedColsBuilder.append(s).append(", ");
        }
        String joinedCols = joinedColsBuilder.toString();

        addBinding(Binding.SELECT,
            new GenericElement("(" + joinedCols.substring(0, joinedCols.length() - 2) + ")"));
        return this;
    }

    /**
     * Bind the objects to the query
     *
     * @param statement The statement to bind the objects to
     *
     * @throws SQLException If an exception is thrown
     */
    private void bindObjects(PreparedStatement statement) throws SQLException {
        int index = 1;
        for (Map.Entry<Binding, ArrayList<QueryElement>> e : this.queryBindings.entrySet()) {
            for (QueryElement el : e.getValue()) {
                for (Object o : el.getBindings()) {
                    statement.setObject(index++, o);
                }
            }
        }
    }

    /**
     * Adds a binding to the query
     *
     * @param type    The type
     * @param element The element
     */
    private void addBinding(Binding type, QueryElement element) {
        ArrayList<QueryElement> elements = this.queryBindings
            .computeIfAbsent(type, k -> new ArrayList<>());
        elements.add(element);
    }
}
