package com.mrkirby153.bfs.query.grammar;

import com.mrkirby153.bfs.query.QueryBuilder;

import java.sql.PreparedStatement;

/**
 * A grammar that compiles a {@link com.mrkirby153.bfs.query.QueryBuilder} into valid SQL
 */
public interface Grammar {

    String compileSelect(QueryBuilder builder);

    String compileUpdate(QueryBuilder builder, String... columnNames);

    String compileDelete(QueryBuilder builder);

    String compileExists(QueryBuilder builder);

    String compileInsert(QueryBuilder builder, String... columnNames);

    String compileInsertMany(QueryBuilder builder, long count,
        String... columnNames);

    void bind(QueryBuilder builder, PreparedStatement statement);
}
