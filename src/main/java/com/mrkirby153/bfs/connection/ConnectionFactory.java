package com.mrkirby153.bfs.connection;

import java.sql.Connection;

/**
 * A factory for retrieving a {@link java.sql.Connection} to use when interacting with the database
 */
public interface ConnectionFactory {

    /**
     * Gets a SQL connection
     *
     * @return The SQL connection
     */
    Connection getConnection();
}
