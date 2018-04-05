package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.sql.elements.GenericElement;
import com.mrkirby153.bfs.sql.elements.WhereElement;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * A query builder for translating model operations into SQL queries
 */
public class QueryBuilder<T extends Model> {

    private Class<T> modelClass;

    private T modelInstance;

    private List<QueryElement> queryElements = new ArrayList<>();
    private List<QueryElement> scopes = new ArrayList<>();

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
        this.scopes.add(new WhereElement(test, column, object));
        return this;
    }

    /**
     * Builds the select statement for the query
     *
     * @param columns The columns to select
     *
     * @return The select statement
     */
    public String buildSelectStatement(String... columns) {
        String colString = buildColumnList(columns);

        this.queryElements.add(new GenericElement("SELECT ("));
        this.queryElements.add(new GenericElement(colString.substring(0, colString.length() - 2)));
        this.queryElements
            .add(new GenericElement(") FROM `" + this.modelInstance.getTable() + "`"));

        return buildQuery();
    }

    /**
     * Builds the column list to use on the query
     *
     * @param columns The columns to use
     *
     * @return The column list
     */
    private String buildColumnList(String[] columns) {
        StringBuilder cols = new StringBuilder();
        if (columns.length == 0) {
            this.modelInstance.getColumnData().keySet().forEach(d -> {
                cols.append(d);
                cols.append(", ");
            });
        } else {
            for (String s : columns) {
                cols.append(s);
                cols.append(", ");
            }
        }
        return cols.toString();
    }

    /**
     * Constructs the query
     *
     * @return The constructed query
     */
    private String buildQuery() {
        StringBuilder queryString = new StringBuilder();
        this.queryElements.forEach(q -> queryString.append(q.getQuery()).append(" "));

        queryString.append(buildScope());
        return queryString.toString().trim();
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
        for (QueryElement e : this.queryElements) {
            for (Object binding : e.getBindings()) {
                statement.setObject(index++, binding);
            }
        }

        for (QueryElement scopes : this.scopes) {
            for (Object binding : scopes.getBindings()) {
                statement.setObject(index++, binding);
            }
        }
    }

    /**
     * Builds up the <code>WHERE</code> scope for the query
     *
     * @return The where clause
     */
    private String buildScope() {
        if (!this.scopes.isEmpty()) {
            StringBuilder scopeBuilder = new StringBuilder();
            scopeBuilder.append("WHERE ");
            this.scopes.forEach(s -> scopeBuilder.append(s.getQuery()).append(" AND "));

            String scope = scopeBuilder.toString();
            return scope.substring(0, scope.length() - 4);
        } else {
            return "";
        }
    }
}
