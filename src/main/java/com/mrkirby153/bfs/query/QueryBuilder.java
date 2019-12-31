package com.mrkirby153.bfs.query;

import com.mrkirby153.bfs.Pair;
import com.mrkirby153.bfs.connection.ConnectionFactory;
import com.mrkirby153.bfs.query.elements.JoinElement;
import com.mrkirby153.bfs.query.elements.OrderElement;
import com.mrkirby153.bfs.query.elements.OrderElement.Direction;
import com.mrkirby153.bfs.query.elements.WhereElement;
import com.mrkirby153.bfs.query.elements.WhereElement.Type;
import com.mrkirby153.bfs.query.event.QueryEvent;
import com.mrkirby153.bfs.query.event.QueryEventListener;
import com.mrkirby153.bfs.query.event.QueryEventManager;
import com.mrkirby153.bfs.query.grammar.Grammar;
import com.mrkirby153.bfs.query.grammar.MySqlGrammar;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * A query builder providing a declarative java-like interface for SQL queries
 */
@Slf4j
@Getter
@RequiredArgsConstructor
public class QueryBuilder {

    public static final Grammar MYSQL_GRAMMAR = new MySqlGrammar();

    private static final String[] operators = new String[]{
        "=", "<", ">", "<=", ">=", "<>", "!=", "<=>", "like", "like binary", "not like", "ilike",
        "&", "|", "^", "<<", ">>", "rlike", "regexp", "not regexp", "~", "~*", "!~*", "similar to",
        "not similar to", "not ilike", "~~*", "!~~*"
    };
    public static ConnectionFactory defaultConnectionFactory;
    // Give 5 threads for running queries
    private static ExecutorService threadPool = Executors
        .newFixedThreadPool(5, new QueryThreadPoolFactory());
    private final Grammar grammar;

    @Setter
    private ConnectionFactory connectionFactory = defaultConnectionFactory;

    /**
     * The table to execute the query on
     */
    private String table;

    /**
     * The where clauses to use
     */
    private List<WhereElement> wheres = new ArrayList<>();

    /**
     * A list of columns to return
     */
    private List<String> columns = new ArrayList<>();

    /**
     * A list of orders on the query
     */
    private List<OrderElement> orders = new ArrayList<>();

    /**
     * A list of joins on the query
     */
    private List<JoinElement> joins = new ArrayList<>();

    /**
     * A list of bindings grouped by the type of statement
     */
    private Map<String, ArrayList<Object>> bindings = new HashMap<>();

    /**
     * The limit of the query
     */
    private Long limit = null;

    /**
     * The offset of the query
     */
    private Long offset = null;

    /**
     * If the query should return distinct values
     */
    private boolean distinct = false;

    @Getter
    private HashMap<QueryEvent.Type, List<QueryEventListener>> eventListeners = new HashMap<>();

    public QueryBuilder() {
        this.grammar = MYSQL_GRAMMAR;
    }

    /**
     * Selects the columns to return
     *
     * @param columns The columns to return
     *
     * @return The query builder
     */
    public QueryBuilder select(String... columns) {
        this.columns.clear();
        this.columns.addAll(Arrays.asList(columns));
        return this;
    }

    /**
     * Sets the table to query
     *
     * @param table The table
     *
     * @return The query builder
     */
    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    public QueryBuilder table(String table) {
        return from(table);
    }

    /**
     * Adds a binding to the query
     *
     * @param section The section to add the bindings
     * @param data    The data to bind
     */
    public void addBinding(String section, Object data) {
        this.bindings.computeIfAbsent(section, s -> new ArrayList<>()).add(data);
    }

    /**
     * Adds a where clause to the query
     *
     * @param column   The column
     * @param operator The comparison operator
     * @param value    The value to compare
     * @param bool     The boolean separator (AND or OR)
     *
     * @return The query builder
     */
    private QueryBuilder where(String column, String operator, Object value, String bool) {
        if (!Arrays.asList(operators).contains(operator.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("The operator %s is not valid", operator));
        }
        if (value == null) {
            // Special case for null values
            return this.whereNull(column, bool);
        }
        WhereElement e = new WhereElement(Type.BASIC, column, bool,
            new Pair<>("operator", operator),
            new Pair<>("value", value));
        this.wheres.add(e);
        this.addBinding("where", value);
        return this;
    }

    public QueryBuilder where(String column, String operator, Object value) {
        return this.where(column, operator, value, "AND");
    }

    public QueryBuilder where(String column, Object value) {
        return this.where(column, "=", value);
    }

    public QueryBuilder orWhere(String column, String operator, Object value) {
        return this.where(column, operator, value, "OR");
    }

    public QueryBuilder orWhere(String column, Object value) {
        return this.orWhere(column, "=", value);
    }

    /**
     * Adds a WHERE NULL or WHERE NOT NULL clause to the query
     *
     * @param column The column
     * @param not    If the clause should check for null or not null
     * @param bool   The boolean operator
     *
     * @return The query  builder
     */
    private QueryBuilder whereNull(String column, boolean not, String bool) {
        WhereElement e = new WhereElement(not ? Type.NOT_NULL : Type.NULL, column, bool);
        this.wheres.add(e);
        return this;
    }

    public QueryBuilder whereNull(String column) {
        return this.whereNull(column, false, "AND");
    }

    public QueryBuilder whereNull(String column, String bool) {
        return this.whereNull(column, false, "AND");
    }

    public QueryBuilder whereNotNull(String column) {
        return this.whereNull(column, true, "AND");
    }

    public QueryBuilder whereNotNull(String column, String bool) {
        return this.whereNull(column, true, bool);
    }

    /**
     * Adds a {@code WHERE IN} clause to the query
     *
     * @param column The column
     * @param values The values of the column
     * @param not    If the query should be not
     * @param bool   The boolean
     *
     * @return The builder
     */
    public QueryBuilder whereIn(String column, Object[] values, boolean not, String bool) {
        WhereElement e = new WhereElement(not ? Type.NOT_IN : Type.IN, column, bool,
            new Pair<>("values", values));
        this.wheres.add(e);
        Arrays.stream(values).forEach(v -> this.addBinding("where", v));
        return this;
    }

    public QueryBuilder whereIn(String column, Object[] values) {
        return this.whereIn(column, values, false, "AND");
    }

    public QueryBuilder whereIn(String column, Object[] values, String bool) {
        return this.whereIn(column, values, false, bool);
    }

    public QueryBuilder whereNotIn(String column, Object[] values) {
        return this.whereIn(column, values, true, "AND");
    }

    public QueryBuilder whereNotIn(String column, Object[] values, String bool) {
        return this.whereIn(column, values, true, bool);
    }

    /**
     * Adds a where sub-query to the query
     *
     * @param column  The column
     * @param builder The query to add
     * @param not     If the query should be inversed (NOT SUB)
     * @param bool    The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereSub(String column, QueryBuilder builder, boolean not, String bool) {
        WhereElement e = new WhereElement(not ? Type.NOT_SUB : Type.SUB, column, bool,
            new Pair<>("builder", builder));
        builder.getBindings().forEach(this::addBinding);
        this.wheres.add(e);
        return this;
    }

    public QueryBuilder whereSub(String column, QueryBuilder builder) {
        return this.whereSub(column, builder, false, "AND");
    }

    public QueryBuilder whereSub(String column, QueryBuilder builder, String bool) {
        return this.whereSub(column, builder, false, bool);
    }

    public QueryBuilder whereNotSub(String column, QueryBuilder builder) {
        return this.whereSub(column, builder, true, "AND");
    }

    public QueryBuilder whereNotSub(String column, QueryBuilder builder, String bool) {
        return this.whereSub(column, builder, true, bool);
    }

    /**
     * Adds an ORDER BY clause to the query
     *
     * @param column    The column to order by
     * @param direction The direction to order by
     */
    public QueryBuilder orderBy(String column, Direction direction) {
        this.orders.add(new OrderElement(column, direction));
        return this;
    }

    /**
     * Sets the limit for this query
     *
     * @param amount The amount of rows to return
     *
     * @return The query builder
     */
    public QueryBuilder limit(long amount) {
        this.limit = amount;
        return this;
    }

    /**
     * Adds a join statement to the query
     *
     * @param type     The type of join
     * @param table    The table eto join
     * @param first    The first column to join
     * @param operator The operator to compare the columns
     * @param second   The second column to join
     *
     * @return The query builder
     */
    public QueryBuilder join(JoinElement.Type type, String table, String first, String operator,
        String second) {
        if (!Arrays.asList(operators).contains(operator.toLowerCase())) {
            throw new IllegalArgumentException(
                String.format("The operator %s was not found", operator));
        }
        this.joins.add(new JoinElement(table, first, operator, second, type));
        return this;
    }

    public QueryBuilder leftJoin(String table, String first, String operator, String second) {
        return this.join(JoinElement.Type.LEFT, table, first, operator, second);
    }

    public QueryBuilder rightJoin(String table, String first, String operator, String second) {
        return this.join(JoinElement.Type.RIGHT, table, first, operator, second);
    }

    public QueryBuilder innerJoin(String table, String first, String operator, String second) {
        return this.join(JoinElement.Type.INNER, table, first, operator, second);
    }

    public QueryBuilder outerJoin(String table, String first, String operator, String second) {
        return this.join(JoinElement.Type.OUTER, table, first, operator, second);
    }

    public QueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Sets the offset for this query
     *
     * @param amount The amount of rows to return
     *
     * @return The query
     */
    public QueryBuilder offset(long amount) {
        this.offset = amount;
        return this;
    }

    public final int update(Pair<String, Object>... data) {
        try {
            return updateAsync(data).get();
        } catch (InterruptedException e) {
            // Ignore
        } catch (ExecutionException e) {
            log.error("Could not execute update", e);
        }
        return -1;
    }

    public final List<DbRow> query() {
        try {
            return queryAsync().get();
        } catch (InterruptedException e) {
            // Ignore
        } catch (ExecutionException e) {
            log.error("Could not execute query", e);
        }
        return Collections.emptyList();
    }

    public CompletableFuture<List<DbRow>> queryAsync() {
        CompletableFuture<List<DbRow>> cf = new CompletableFuture<>();
        threadPool.submit(() -> {
            QueryEventManager.callEvents(QueryEvent.Type.PRE_GET, this);
            String query = this.grammar.compileSelect(this);
            try (Connection c = connectionFactory.getConnection(); PreparedStatement ps = c
                .prepareStatement(query)) {
                grammar.bind(this, ps);
                log.trace("Executing SELECT: {}", ps);
                try (ResultSet rs = ps.executeQuery()) {
                    QueryEventManager.callEvents(QueryEvent.Type.POST_GET, this);
                    cf.complete(parse(rs));
                }
            } catch (SQLException e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    public final CompletableFuture<Integer> updateAsync(Pair<String, Object>... data) {
        CompletableFuture<Integer> cf = new CompletableFuture<>();
        threadPool.submit(() -> {
            QueryEventManager.callEvents(QueryEvent.Type.PRE_UPDATE, this);
            List<Object> bindings = this.bindings.computeIfAbsent("update", a -> new ArrayList<>());
            bindings.addAll(Arrays.stream(data).map(Pair::getSecond).collect(Collectors.toList()));
            String query = this.grammar.compileUpdate(this,
                Arrays.stream(data).map(Pair::getFirst).toArray(String[]::new));
            try (Connection c = connectionFactory.getConnection();
                PreparedStatement ps = c.prepareStatement(query)) {
                grammar.bind(this, ps);
                log.trace("Executing UPDATE: {}", ps);
                QueryEventManager.callEvents(QueryEvent.Type.POST_UPDATE, this);
                cf.complete(ps.executeUpdate());
            } catch (SQLException e) {
                cf.completeExceptionally(e);
            }
        });
        return cf;
    }

    public  boolean delete() {
        if (!QueryEventManager.callEvents(QueryEvent.Type.PRE_DELETE, this)) {
            return true;
        }
        String query = this.grammar.compileDelete(this);
        try (Connection c = connectionFactory.getConnection(); PreparedStatement ps = c
            .prepareStatement(query)) {
            grammar.bind(this, ps);
            log.trace("Executing DELETE: {}", ps);
            boolean success = ps.executeUpdate() > 0;
            QueryEventManager.callEvents(QueryEvent.Type.POST_DELETE, this);
            return success;
        } catch (SQLException e) {
            log.error("Error when deleting", e);
        }
        return false;
    }

    @SafeVarargs
    public final void insert(Pair<String, Object>... data) {
        if (!QueryEventManager.callEvents(QueryEvent.Type.PRE_CREATE, this)) {
            return;
        }
        Arrays.stream(data).map(Pair::getSecond).forEach(d -> addBinding("insert", d));
        String query = this.grammar
            .compileInsert(this, Arrays.stream(data).map(Pair::getFirst).toArray(String[]::new));
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query)) {
            this.grammar.bind(this, ps);
            log.trace("Executing INSERT: {}", ps);
            ps.executeUpdate();
            QueryEventManager.callEvents(QueryEvent.Type.POST_CREATE, this);
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public final long insertWithGenerated(Pair<String, Object>... data) {
        if (!QueryEventManager.callEvents(QueryEvent.Type.PRE_CREATE, this)) {
            return 0;
        }
        Arrays.stream(data).map(Pair::getSecond).forEach(d -> addBinding("insert", d));
        String query = this.grammar
            .compileInsert(this, Arrays.stream(data).map(Pair::getFirst).toArray(String[]::new));
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            this.grammar.bind(this, ps);
            log.trace("Executing INSERT (with generated): " + ps);
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long generated = rs.getLong(1);
                    log.trace("Returned generated value {}", generated);
                    QueryEventManager.callEvents(QueryEvent.Type.POST_CREATE, this);
                    return generated;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public final int insertBulk(List<Map<String, Object>> data) {
        return insertBulk(data, false).size();
    }

    public final List<Long> insertBulkWithGenerated(List<Map<String, Object>> data) {
        return insertBulk(data, true);
    }

    public final void registerListener(QueryEvent.Type type, QueryEventListener listener) {
        this.eventListeners.computeIfAbsent(type, t -> new ArrayList<>()).add(listener);
    }

    public final void unregisterListener(QueryEvent.Type type, QueryEventListener listener) {
        this.eventListeners.computeIfAbsent(type, t -> new ArrayList<>()).remove(listener);
    }

    private List<Long> insertBulk(List<Map<String, Object>> data, boolean generated) {
        if (data.size() == 0) {
            throw new IllegalArgumentException("Can't insert nothing");
        }
        int colCount = data.get(0).size();
        for (Map<String, Object> d : data) {
            if (d.size() != colCount) {
                throw new IllegalArgumentException(
                    "Inconsistent column count. Expected " + colCount + " got " + d.size());
            }
        }
        if (!QueryEventManager.callEvents(QueryEvent.Type.PRE_CREATE, this)) {
            return Collections.emptyList();
        }
        data.stream().flatMap(a -> a.entrySet().stream()).map(Entry::getValue)
            .forEach(d -> addBinding("insert", d));
        String query = this.grammar.compileInsertMany(this, data.size(),
            data.get(0).keySet().toArray(new String[0]));
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query,
                generated ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            this.grammar.bind(this, ps);
            log.trace("Executing BULK INSERT (With generated? {}): {}", generated, ps);
            List<Long> gen = new ArrayList<>();
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                while (rs.next()) {
                    gen.add(rs.getLong(1));
                }
                QueryEventManager.callEvents(QueryEvent.Type.POST_CREATE, this);
                return gen;
            }
        } catch (SQLException e) {
            log.error("Error when inserting bulk", e);
        }
        return Collections.emptyList();
    }

    private List<DbRow> parse(ResultSet rs) throws SQLException {
        ArrayList<DbRow> data = new ArrayList<>();
        ResultSetMetaData md = rs.getMetaData();
        while (rs.next()) {
            DbRow row = new DbRow();
            for (int i = 1; i <= md.getColumnCount(); i++) {
                String col = md.getColumnLabel(i);
                row.put(col, rs.getObject(col));
            }
            data.add(row);
        }
        return data;
    }
}
