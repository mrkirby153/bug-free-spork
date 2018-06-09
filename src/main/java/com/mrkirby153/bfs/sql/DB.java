package com.mrkirby153.bfs.sql;

import org.intellij.lang.annotations.Language;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class DB {

    /**
     * Gets the first row of the query
     *
     * @param query  The SQL query
     * @param params The parameters to bind into the query
     * @return The first row
     */
    public static DbRow getFirstRow(@Language("MySQL") String query, Object... params) {
        QueryBuilder builder = new QueryBuilder();
        List<DbRow> rows = builder.raw(query, params);
        if (rows.size() == 0)
            return null;
        else
            return rows.get(0);
    }

    /**
     * Gets the first row/column returned by the query
     *
     * @param query  The query
     * @param params The parameters to bind into the query
     * @param <T>
     * @return The value of the first row's first column
     */
    public static <T> T getFirstColumn(@Language("MySQL") String query, Object... params) {
        QueryBuilder builder = new QueryBuilder();
        List<DbRow> rows = builder.raw(query, params);
        DbRow first = rows.size() > 0 ? rows.get(0) : null;
        if (first == null)
            return null;
        List<String> cols = new ArrayList<>(first.keySet());
        if (cols.size() == 0)
            throw new IllegalStateException("Provided query did not return any columns!");
        return first.get(cols.get(0));
    }

    /**
     * Gets the values of the first column of the query
     *
     * @param query  The query
     * @param params The params to bind
     * @param <T>
     * @return The values, or an empty set if none were returned
     */
    public static <T> List<T> getFirstColumnValues(@Language("MySQL") String query, Object... params) {
        QueryBuilder builder = new QueryBuilder();
        List<DbRow> rows = builder.raw(query, params);
        DbRow first = rows.size() > 0 ? rows.get(0) : null;
        if (first == null)
            return new ArrayList<>();
        List<String> cols = new ArrayList<>(first.keySet());
        if (cols.size() == 0)
            throw new IllegalStateException("Provided query did not return any columns!");
        String firstColName = cols.get(0);
        List<T> list = new ArrayList<>();
        rows.forEach(row -> list.add(row.get(firstColName)));
        return list;
    }

    /**
     * Executes raw SQL and returns a resu;t
     *
     * @param query  The query
     * @param params The params to bind
     * @return A list of Db Rows or an empty list
     */
    public static List<DbRow> getResults(@Language("MySQL") String query, Object... params) {
        return new QueryBuilder().raw(query, params);
    }

    /**
     * Executes an update
     *
     * @param query  The query
     * @param params The object to bind
     * @return The number of rows affected
     */
    public static int executeUpdate(@Language("MySQL") String query, Object... params) {
        try (Connection con = QueryBuilder.connectionFactory.getConnection()) {
            PreparedStatement statement = con.prepareStatement(query);
            int index = 1;
            for (Object o : params) {
                statement.setObject(index++, o);
            }
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * Executes an insert
     *
     * @param query  The query
     * @param params The object to bind
     * @return The generated ID or null
     */
    public static Long executeInsert(@Language("MySQL") String query, Object... params) {
        try (Connection con = QueryBuilder.connectionFactory.getConnection()) {
            PreparedStatement statement = con.prepareStatement(query);
            int index = 1;
            for (Object o : params) {
                statement.setObject(index++, o);
            }
            statement.executeLargeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs == null)
                    return null;
                Long result = null;
                if (rs.next())
                    result = rs.getLong(1);
                return result;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }
}
