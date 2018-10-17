package com.mrkirby153.bfs.sql.grammars;

import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.Pair;

import java.sql.PreparedStatement;
import java.util.List;

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
     * Compiles an update to the statement
     *
     * @param builder The builder
     *
     * @return The compiled update prepared statement
     */
    String compileUpdate(QueryBuilder builder, Pair... pairs);


    /**
     * Compiles a delete statement
     *
     * @param builder The builder
     *
     * @return A delete prepared statement
     */
    String compileDelete(QueryBuilder builder);

    /**
     * Compiles an exists query
     *
     * @param builder The builder
     *
     * @return The exists query
     */
    String compileExists(QueryBuilder builder);

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
     * Compiles an insert statement to insert multiple rows into the database
     *
     * @param builder The builder
     * @param data    A list of a list of {@link Pair} of the data to insert
     *
     * @return The statement
     */
    String compileInsertMany(QueryBuilder builder, List<List<Pair>> data);

    void bind(QueryBuilder builder, PreparedStatement statement);
}
