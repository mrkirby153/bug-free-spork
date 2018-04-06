package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.Pair;

import java.sql.PreparedStatement;

/**
 * A grammar that compiles a {@link QueryBuilder} into valid SQL
 */
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
     * @param builder   The Query builder
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

    /**
     * Binds the objects to a the prepared statement
     *
     * @param builder   The query builder
     * @param statement The statement
     * @param pairs     The data to bind
     */
    void bindUpdate(QueryBuilder builder, PreparedStatement statement, Pair... pairs);


    /**
     * Compiles a delete statement
     *
     * @param builder The builder
     *
     * @return A delete prepared statement
     */
    String compileDelete(QueryBuilder builder);

    /**
     * Binds the obejcts to a delete statement
     *
     * @param builder   The builder
     * @param statement The statement
     */
    void bindDelete(QueryBuilder builder, PreparedStatement statement);

    /**
     * Compiles an exists query
     *
     * @param builder The builder
     *
     * @return The exists query
     */
    String compileExists(QueryBuilder builder);

    /**
     * Binds objects to an exists query
     *
     * @param builder   The builder
     * @param statement The statement
     */
    void bindExists(QueryBuilder builder, PreparedStatement statement);

    /**
     * Compiles an insert statement
     *
     * @param builder The builder
     * @param data    The data
     *
     * @return The statement
     */
    String compileInsert(QueryBuilder builder, Pair... data);

    /**
     * Bind objects in an insert statement
     *
     * @param builder   The builder
     * @param statement The statement
     * @param data      The data
     */
    void bindInsert(QueryBuilder builder, PreparedStatement statement, Pair... data);
}
