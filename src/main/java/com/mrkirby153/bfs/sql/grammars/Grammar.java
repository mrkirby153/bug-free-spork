package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;

import java.sql.PreparedStatement;

public interface Grammar {

    /**
     * Builds a select statement out of the
     *
     * @param builder The builder
     *
     * @return The compiled select prepared statement
     */
    String compileSelect(QueryBuilder builder);

    /**
     * Binds all the objects to the statement
     *
     * @param statement The statement to bind into
     */
    void bindSelect(PreparedStatement statement);
}
