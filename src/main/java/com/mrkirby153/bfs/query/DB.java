package com.mrkirby153.bfs.query;

import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Slf4j
public class DB {

    /**
     * Gets the first row of the query
     *
     * @param query  The SQL query
     * @param params The parameters to bind into the query
     *
     * @return The first row
     */
    @Nullable
    public static DbRow getFirstRow(@Language("SQL") String query, Object... params) {
        QueryBuilder qb = new QueryBuilder();
        List<DbRow> rows = qb.raw(query, params);
        if (rows.size() == 0) {
            return null;
        } else {
            return rows.get(0);
        }
    }

    /**
     * Gets the first row of the query async
     *
     * @param query  The query
     * @param params The parameters to bind into the query
     *
     * @return The first row
     *
     * @see #getFirstRow(String, Object...)
     */
    @NotNull
    public static CompletableFuture<DbRow> getFirstRowAsync(@Language("SQL") String query,
        Object... params) {
        QueryBuilder qb = new QueryBuilder();
        return qb.rawAsync(query, params).thenApply(rows -> {
            if (rows.size() == 0) {
                return null;
            } else {
                return rows.get(0);
            }
        });
    }

    /**
     * Gets the first row and column of the query
     *
     * @param query  The SQL query
     * @param params The parameters to bind to the query
     *
     * @return The value of the first row's first column
     */
    @Nullable
    public static <T> T getFirstColumn(@Language("SQL") String query, Object... params) {
        try {
            DB.<T>getFirstColumnAsync(query, params).get();
        } catch (InterruptedException e) {
            // Ignored
        } catch (ExecutionException e) {
            log.error("An exception occurred when querying", e.getCause());
        }
        return null;
    }

    /**
     * Gets the first row and column of the query async
     *
     * @param query  The SQL query
     * @param params The parameters to build the query
     *
     * @return The value of the first row's first column
     *
     * @see #getFirstColumn(String, Object...)
     */
    @NotNull
    public static <T> CompletableFuture<T> getFirstColumnAsync(@Language("SQL") String query,
        Object... params) {
        return new QueryBuilder().rawAsync(query, params).thenApply(rows -> {
            DbRow first = rows.size() > 0 ? rows.get(0) : null;
            if (first == null) {
                return null;
            }
            List<String> cols = new ArrayList<>(first.keySet());
            if (cols.size() == 0) {
                throw new IllegalStateException("Provided query did not return any columns");
            }
            return first.get(cols.get(0));
        });
    }

    /**
     * Gets the value of the first column in the query
     *
     * @param query  The SQL query
     * @param params The parameters to bind to the query
     *
     * @return The values, or an empty list if none were returned
     */
    @NotNull
    public static <T> List<T> getFirstColumnValues(@Language("SQL") String query,
        Object... params) {
        try {
            return DB.<T>getFirstColumnValuesAsync(query, params).get();
        } catch (InterruptedException e) {
            // Ignored
        } catch (ExecutionException e) {
            log.error("An exception occurred when querying", e.getCause());
        }
        return new ArrayList<>();
    }

    /**
     * Gets the value of the first column in the query async
     *
     * @param query  The SQL query
     * @param params The parameters to bind to the query
     *
     * @return The values, or an empty list if none were returned
     *
     * @see #getFirstColumnValues(String, Object...)
     */
    public static <T> CompletableFuture<List<T>> getFirstColumnValuesAsync(
        @Language("SQL") String query, Object... params) {
        return new QueryBuilder().rawAsync(query, params).thenApply(rows -> {
            DbRow first = rows.size() > 0 ? rows.get(0) : null;
            if (first == null) {
                return new ArrayList<>();
            }
            List<String> cols = new ArrayList<>(first.keySet());
            if (cols.size() == 0) {
                throw new IllegalStateException("Provided query did not return any columns");
            }
            String firstColName = cols.get(0);
            List<T> list = new ArrayList<>();
            rows.forEach(row -> list.add(row.get(firstColName)));
            return list;
        });
    }

    /**
     * Executes a raw SQL query and returns a result
     *
     * @param query  The query
     * @param params The params to bind
     *
     * @return A list of Db rows
     */
    @NotNull
    public static List<DbRow> raw(@Language("SQL") String query, Object... params) {
        return new QueryBuilder().raw(query, params);
    }

    /**
     * Executes a raw SQL query async and returns a result
     *
     * @param query  The query
     * @param params The parameters to bind
     *
     * @return A list of DB rows
     *
     * @see #raw(String, Object...)
     */
    @NotNull
    public static CompletableFuture<List<DbRow>> rawAsync(@Language("SQL") String query,
        Object... params) {
        return new QueryBuilder().rawAsync(query, params);
    }

    /**
     * Executes an update
     *
     * @param query  The query
     * @param params The objects to bind
     *
     * @return The number of rows affected
     */
    public static int executeUpdate(@Language("SQL") String query, Object... params) {
        try (Connection connection = QueryBuilder.defaultConnectionFactory.getConnection();
            PreparedStatement ps = connection.prepareStatement(query)) {
            int i = 1;
            for (Object o : params) {
                ps.setObject(i++, o);
            }
            return ps.executeUpdate();
        } catch (SQLException e) {
            log.error("Could not execute update", e);
        }
        return 0;
    }

    /**
     * Executes an update async
     *
     * @param query  The query
     * @param params The objects to bind
     *
     * @return The number of rows affected
     *
     * @see #executeUpdate(String, Object...)
     */
    @NotNull
    public static CompletableFuture<Integer> executeUpdateAsync(@Language("SQL") String query,
        Object... params) {
        return CompletableFuture
            .supplyAsync(() -> executeUpdate(query, params), QueryBuilder.getThreadPool());
    }

    /**
     * Executes an insert
     *
     * @param query  The query
     * @param params The object to bind
     *
     * @return The generated ID or null
     */
    @Nullable
    public static Long executeInsert(@Language("SQL") String query, Object... params) {
        try (Connection con = QueryBuilder.defaultConnectionFactory
            .getConnection(); PreparedStatement statement = con.prepareStatement(query,
            Statement.RETURN_GENERATED_KEYS)) {
            int i = 0;
            for (Object o : params) {
                statement.setObject(i++, o);
            }
            statement.executeUpdate();
            try (ResultSet rs = statement.getGeneratedKeys()) {
                if (rs == null) {
                    return null;
                }
                Long result = null;
                if (rs.next()) {
                    result = rs.getLong(1);
                }
                return result;
            }
        } catch (SQLException e) {
            log.error("Could not execute insert", e);
        }
        return null;
    }


    /**
     * Executes an insert
     *
     * @param query  The query
     * @param params The object to bind
     *
     * @return The generated id or null
     *
     * @see #executeInsert(String, Object...)
     */
    @NotNull
    public static CompletableFuture<Long> executeInsertAsync(@Language("SQL") String query,
        Object... params) {
        return CompletableFuture
            .supplyAsync(() -> executeInsert(query, params), QueryBuilder.getThreadPool());
    }

}
