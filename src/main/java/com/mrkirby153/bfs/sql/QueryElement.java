package com.mrkirby153.bfs.sql;

import java.util.List;

/**
 * An element in an SQL query
 */
public interface QueryElement {

    /**
     * Gets the SQL
     *
     * @return The sql
     */
    String getQuery();

    /**
     * Gets any object bindings for this query
     *
     * @return The query
     */
    List<Object> getBindings();
}
