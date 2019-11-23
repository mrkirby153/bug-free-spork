package com.mrkirby153.bfs.connection;

import lombok.RequiredArgsConstructor;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

/**
 * A basic MySQL connection to the database
 */
@RequiredArgsConstructor
public class BasicMySQLConnectionFactory implements ConnectionFactory {

    private final String host;
    private final int port;
    private final String database;
    private final String username;
    private final String password;

    @Override
    public Connection getConnection() {
        try {
            Class.forName("com.mysql.jdbc.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException("Could not find the MySQL driver on the classpath!");
        }

        try {
            return DriverManager.getConnection(getConnectionUrl(), this.username, this.password);
        } catch (SQLException e) {
            throw new IllegalStateException("Could not create a connection to the database", e);
        }
    }

    /**
     * Gets the connection URL for the database
     *
     * @return The connection URL
     */
    public String getConnectionUrl() {
        return String.format("jdbc:mysql://%s:%d/%s", this.host, this.port, this.database);
    }
}
