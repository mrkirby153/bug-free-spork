package com.mrkirby153.bugfreespork;

import java.sql.Connection;

/**
 * A factory for getting {@link Connection} to use when interacting with the database
 */
public interface ConnectionFactory {

    Connection getConnection();
}
