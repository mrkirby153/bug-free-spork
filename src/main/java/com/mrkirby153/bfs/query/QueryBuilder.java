package com.mrkirby153.bfs.query;

import com.mrkirby153.bfs.Pair;
import com.mrkirby153.bfs.query.elements.JoinElement;
import com.mrkirby153.bfs.query.elements.OrderElement;
import com.mrkirby153.bfs.query.elements.OrderElement.Direction;
import com.mrkirby153.bfs.query.elements.WhereElement;
import com.mrkirby153.bfs.query.elements.WhereElement.Type;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A query builder providing a declarative java-like interface for SQL queries
 */
@Slf4j
@Getter
public class QueryBuilder {

    private static final String[] operators = new String[]{
        "=", "<", ">", "<=", ">=", "<>", "!=", "<=>", "like", "like binary", "not like", "ilike",
        "&", "|", "^", "<<", ">>", "rlike", "regexp", "not regexp", "~", "~*", "!~*", "similar to",
        "not similar to", "not ilike", "~~*", "!~~*"
    };

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

    public int update(Pair<String, Object>... data) {
        // TODO: 11/23/19 Implement
        return 0;
    }

    public List<DbRow> query() {
        // TODO: 11/23/19 Implement
        return null;
    }

    public boolean delete() {
        // TODO: 11/23/19 Implement
        return false;
    }

    public void insert(Pair<String, Object>... data) {
        // TODO: 11/23/19 Implement
    }

    public long insertWithGenerated(Pair<String, Object>... data) {
        // TODO: 11/23/19 Implement
        return -1;
    }

    public int insertBulk(List<Map<String, Object>> data) {
        // TODO: 11/23/19 Implement
        return -1;
    }

    public List<Long> insertBulkWithGenerated(List<Map<String, Object>> data) {
        // TODO: 11/23/19 Implement
        return null;
    }
}
