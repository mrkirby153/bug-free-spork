package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.ConnectionFactory;

/**
 * A model in the database
 */
public class Model {

    /**
     * The connection factory used for the database
     */
    private static ConnectionFactory connectionFactory;


    /**
     * Sets the connection factory used globally
     *
     * @param factory The connection factory
     */
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

}
