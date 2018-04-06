package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.Pair;

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
    void bindSelect(QueryBuilder builder, PreparedStatement statement);


    /**
     * Compiles an update to the statement
     *
     * @param builder The builder
     *
     * @return The compiled update prepared statement
     */
    String compileUpdate(QueryBuilder builder, Pair... pairs);

    void bindUpdate(QueryBuilder builder, PreparedStatement statement, Pair... pairs);


    String compileDelete(QueryBuilder builder);

    void bindDelete(QueryBuilder builder, PreparedStatement statement);

    String compileExists(QueryBuilder builder);

    void bindExists(QueryBuilder builder, PreparedStatement statement);

    String compileInsert(QueryBuilder builder, Pair... data);

    void bindInsert(QueryBuilder builder, PreparedStatement statement, Pair... data);
}
