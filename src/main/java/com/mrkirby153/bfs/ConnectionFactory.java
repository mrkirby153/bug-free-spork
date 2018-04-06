package com.mrkirby153.bfs;

import java.sql.Connection;

/**
 * A factory for retrieving a {@link Connection} to use when interacting with the database
 */
public interface ConnectionFactory {

    /**
     * Gets an SQL connection
     *
     * @return The SQL connection
     */
    Connection getConnection();
}
