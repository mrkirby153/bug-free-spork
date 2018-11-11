package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.ConnectionFactory;
import com.mrkirby153.bfs.Tuple;
import com.mrkirby153.bfs.sql.elements.JoinElement;
import com.mrkirby153.bfs.sql.elements.JoinElement.Type;
import com.mrkirby153.bfs.sql.elements.OrderElement;
import com.mrkirby153.bfs.sql.elements.Pair;
import com.mrkirby153.bfs.sql.elements.WhereElement;
import com.mrkirby153.bfs.sql.grammars.Grammar;
import com.mrkirby153.bfs.sql.grammars.MySqlGrammar;
import org.intellij.lang.annotations.Language;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

/**
 * A query builder for translating model operations into SQL queries
 */
public class QueryBuilder {

    private static final Logger logger = LoggerFactory.getLogger("bfs-querybuilder");

    private static final String[] operators = new String[]{
        "=", "<", ">", "<=", ">=", "<>", "!=", "<=>", "like", "like binary", "not like", "ilike",
        "&", "|", "^", "<<", ">>", "rlike", "regexp", "not regexp", "~", "~*", "!~*", "similar to",
        "not similar to", "not ilike", "~~*", "!~~*"
    };
    public static ConnectionFactory connectionFactory = null;

    public static boolean logQueries = false;

    /**
     * The table to execute the query on
     */
    private String table;

    /**
     * The where clauses to use
     */
    private ArrayList<WhereElement> wheres = new ArrayList<>();

    /**
     * A list of columns to return
     */
    private String[] columns = new String[0];

    /**
     * A collection of all the order statements
     */
    private ArrayList<OrderElement> orders = new ArrayList<>();

    /**
     * A collection of various join statements
     */
    private ArrayList<JoinElement> joins = new ArrayList<>();

    private HashMap<String, ArrayList<Object>> bindings = new HashMap<>();

    /**
     * The limit of the query to return
     */
    private Long limit = null;

    /**
     * The offset of the query
     */
    private Long offset = null;

    /**
     * The current grammar that is being used to construct the SQL query
     */
    private Grammar grammar;

    /**
     * If the query should return distinct values
     */
    private boolean distinct = false;

    public QueryBuilder() {
        this.grammar = new MySqlGrammar();
    }

    public QueryBuilder(Grammar grammar) {
        this.grammar = grammar;
    }

    /**
     * Gets a list of all the valid operators
     *
     * @return An operator list
     */
    public static String[] getOperators() {
        return operators;
    }

    /**
     * Selects the columns to return when querying
     *
     * @param columns A list of columns to return
     *
     * @return The query builder
     */
    public QueryBuilder select(String... columns) {
        this.columns = columns;
        return this;
    }

    /**
     * Selects the table to query
     *
     * @param table The table to query
     *
     * @return The query builder
     */
    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    /**
     * Selects the table to query
     *
     * @param table The table to query
     *
     * @return The query builder
     */
    public QueryBuilder table(String table) {
        this.table = table;
        return this;
    }

    /**
     * Gets the list of all join elements
     *
     * @return The list of join elements
     */
    public ArrayList<JoinElement> getJoins() {
        return joins;
    }

    /**
     * Gets the limit (maximum number of rows to return) of this query
     *
     * @return The limit or null if there is no limit
     */
    public Long getLimit() {
        return limit;
    }

    /**
     * Gets the offset of the query (number of rows to skip)
     *
     * @return The offset or null if there is no offset
     */
    public Long getOffset() {
        return offset;
    }

    /**
     * Gets if the query should return distinct values
     *
     * @return True if the query should return only distinct values
     */
    public boolean isDistinct() {
        return distinct;
    }

    public void addBinding(String section, Object data) {
        ArrayList<Object> bindings = this.bindings.computeIfAbsent(section, k -> new ArrayList<>());
        bindings.add(data);
    }

    public void addBinding(Object data) {
        this.addBinding("where", data);
    }

    public List<Object> getBindings() {
        List<Object> bindings = new ArrayList<>();
        this.bindings.forEach((k, v) -> bindings.addAll(v));
        return bindings;
    }

    /**
     * Adds a <code>WHERE</code> clause to the query
     *
     * @param column   The column to use in the clause
     * @param operator The operator to use when comparing
     * @param value    The value to compare
     * @param bool     The boolean separator (AND or OR)
     *
     * @return The query builder
     *
     * @throws IllegalArgumentException If the operator provided is not a valid operator
     */
    public QueryBuilder where(String column, String operator, Object value, String bool) {
        if (!Arrays.asList(operators).contains(operator.toLowerCase())) {
            throw new IllegalArgumentException("The operator " + operator + " is not valid!");
        }
        // Special override for null values
        if (value == null) {
            this.whereNull(column, operator.equalsIgnoreCase("!="), bool);
            return this;
        }
        WhereElement e = new WhereElement("Basic", new Tuple<>("column", column),
            new Tuple<>("operator", operator), new Tuple<>("boolean", bool),
            new Tuple<>("value", value));
        this.wheres.add(e);
        this.addBinding("where", value);
        return this;
    }

    /**
     * Adds a <code>WHERE</code> clause to the query
     *
     * @param column   The column to use in the clause
     * @param operator The operator to use when comparing
     * @param value    The value to compare against
     *
     * @return The query builder
     */
    public QueryBuilder where(String column, String operator, Object value) {
        return this.where(column, operator, value, "AND");
    }

    /**
     * Adds a <code>WHERE</code> clause to the query
     *
     * @param column The column to use in the query
     * @param value  The value to compare against
     *
     * @return The query builder
     */
    public QueryBuilder where(String column, Object value) {
        return this.where(column, "=", value);
    }

    /**
     * Adds a <code>WHERE</code> clause with a boolean separator of OR to the query
     *
     * @param column   The column to use in the query
     * @param operator The operator to use
     * @param value    The value to compare against
     *
     * @return The query builder
     */
    public QueryBuilder orWhere(String column, String operator, Object value) {
        return this.where(column, operator, value, "OR");
    }

    /**
     * Adds a <code>WHERE</code> clause with a boolean separator of OR to the query
     *
     * @param column The column to use in the query
     * @param value  The value to compare against
     *
     * @return The query builder
     */
    public QueryBuilder orWhere(String column, Object value) {
        return this.where(column, "=", value, "OR");
    }

    /**
     * Adds a WHERE clause checking if a column is null
     *
     * @param column The column
     * @param not    If the column should be not null
     * @param bool   The boolean separator
     *
     * @return The query builder
     */
    private QueryBuilder whereNull(String column, boolean not, String bool) {
        WhereElement e = new WhereElement(not ? "NotNull" : "Null", new Tuple<>("column", column),
            new Tuple<>("boolean", bool));
        this.wheres.add(e);
        return this;
    }

    /**
     * Adds a where clause checking if a column is null
     *
     * @param column The column
     *
     * @return The query builder
     */
    public QueryBuilder whereNull(String column) {
        return whereNull(column, "AND");
    }

    /**
     * Adds a where clause checking if a columns is null
     *
     * @param column The column
     * @param bool   The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereNull(String column, String bool) {
        return whereNull(column, false, bool);
    }

    /**
     * Adds a where clause checking if a column is not null
     *
     * @param column The column
     * @param bool   The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereNotNull(String column, String bool) {
        return whereNull(column, true, bool);
    }

    /**
     * Adds a where clause checking if a column is not null
     *
     * @param column The column
     *
     * @return The query builder
     */
    public QueryBuilder whereNotNull(String column) {
        return whereNotNull(column, "AND");
    }

    /**
     * Adds a WHERE IN clause to the query
     *
     * @param column The column
     * @param values The values the column must be equal to
     * @param bool   The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereIn(String column, Object[] values, String bool) {
        WhereElement e = new WhereElement("In", new Tuple<>("column", column),
            new Tuple<>("values", values), new Tuple<>("boolean", bool));
        Arrays.stream(values).forEach(this::addBinding);
        this.wheres.add(e);
        return this;
    }

    /**
     * Adds a WHERE IN clause to the query
     *
     * @param column The column
     * @param values The values of the column
     *
     * @return The query builder
     */
    public QueryBuilder whereIn(String column, Object[] values) {
        return this.whereIn(column, values, "AND");
    }

    /**
     * Adds a WHERE NOT IN clause to the query
     *
     * @param column THe column
     * @param values The values of the column
     * @param bool   The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereNotIn(String column, Object[] values, String bool) {
        WhereElement e = new WhereElement("NotIn", new Tuple<>("column", column),
            new Tuple<>("values", values), new Tuple<>("boolean", bool));
        Arrays.stream(values).forEach(this::addBinding);
        this.wheres.add(e);
        return this;
    }

    /**
     * Adds a WHERE NOT IN clause to the query
     *
     * @param column The column
     * @param values The values of the column
     *
     * @return The query builder
     */
    public QueryBuilder whereNotIn(String column, Object[] values) {
        return this.whereNotIn(column, values, "AND");
    }

    /**
     * Adds a where sub-query to the query
     *
     * @param column  The column
     * @param builder The query to add
     * @param not     If it should exclude the values returned by the previous query
     * @param bool    The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereSub(String column, QueryBuilder builder, boolean not, String bool) {
        WhereElement e = new WhereElement(not ? "NotSub" : "Sub", new Tuple<>("query", builder),
            new Tuple<>("boolean", bool), new Tuple<>("column", column));
        builder.getBindings().forEach(this::addBinding);
        this.wheres.add(e);
        return this;
    }

    /**
     * Adds a where sub-query to the query
     *
     * @param column  The column
     * @param builder The query to add
     * @param bool    The boolean separator
     *
     * @return The query builder
     */
    public QueryBuilder whereSub(String column, QueryBuilder builder, String bool) {
        return whereSub(column, builder, false, bool);
    }

    /**
     * Adds a where sub-query to the query
     *
     * @param column  The column
     * @param builder The query to add
     *
     * @return The query builder
     */
    public QueryBuilder whereSub(String column, QueryBuilder builder) {
        return whereSub(column, builder, "AND");
    }

    /**
     * Adds an <code>ORDER BY</code> clause to the query
     *
     * @param column    The column to order by
     * @param direction The direction to order by (ASC or DESC)
     *
     * @return The query builder
     */
    public QueryBuilder orderBy(String column, String direction) {
        this.orders.add(new OrderElement(column, direction));
        return this;
    }

    /**
     * Sets the limit for this sql query
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
     * Sets the number of rows to skip
     *
     * @param amount The amount of rows to skip
     *
     * @return The query builder
     */
    public QueryBuilder offset(long amount) {
        this.offset = amount;
        return this;
    }

    /**
     * Updates the data in the database with the provided parameters
     *
     * @param data The data to update in the database
     *
     * @return The amount of rows returned or -1 if the query fails
     */
    public int update(Pair... data) {
        Arrays.stream(data).map(Pair::getValue).forEach(d -> addBinding("update", d));
        String query = this.grammar.compileUpdate(this, data);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement statement = con.prepareStatement(query)) {
            grammar.bind(this, statement);
            if (logQueries) {
                logger.debug("Executing UPDATE: " + statement);
            }
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Executes the query and returns a {@link ResultSet}
     *
     * @return A list of {@link DbRow}
     */
    public List<DbRow> query() {
        String query = this.grammar.compileSelect(this);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement statement = con.prepareStatement(query)) {
            grammar.bind(this, statement);

            if (logQueries) {
                logger.debug("Executing SELECT: " + statement);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return parse(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Executes raw SQL as a {@link PreparedStatement}
     *
     * @param sql  The raw SQL to execute
     * @param data A list of bindings for the statement
     *
     * @return A list of {@link DbRow}s
     */
    public List<DbRow> raw(@Language("MySQL") String sql, Object... data) {
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement statement = con.prepareStatement(sql)) {
            int index = 1;
            for (Object o : data) {
                statement.setObject(index++, o);
            }
            if (logQueries) {
                logger.debug("Executing raw query: " + statement);
            }
            try (ResultSet rs = statement.executeQuery()) {
                return parse(rs);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Executes a <code>DELETE</code> operation on the query
     *
     * @return True if the delete was successful
     */
    public boolean delete() {
        String query = this.grammar.compileDelete(this);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query)) {
            this.grammar.bind(this, ps);
            if (logQueries) {
                logger.debug("Executing DELETE: " + ps);
            }
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Checks if the query exists
     *
     * @return True if the query returns a result that exists, false if otherwise
     */
    public boolean exists() {
        String query = this.grammar.compileExists(this);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query)) {
            this.grammar.bind(this, ps);

            if (logQueries) {
                logger.debug("Executing EXISTS: " + ps);
            }
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getBoolean("exists");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * Inserts the data into the database
     *
     * @param data The data to insert
     */
    public void insert(Pair... data) {
        Arrays.stream(data).map(Pair::getValue).forEach(d -> addBinding("insert", d));
        String query = this.grammar.compileInsert(this, data);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query)) {
            this.grammar.bind(this, ps);
            if (logQueries) {
                logger.debug("Executing INSERT: " + ps);
            }
            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Inserts the data into the database, returning the primary key
     *
     * @param data The data to insert
     *
     * @return The primary key
     */
    public long insertWithGenerated(Pair... data) {
        Arrays.stream(data).map(Pair::getValue).forEach(d -> addBinding("insert", d));
        String query = this.grammar.compileInsert(this, data);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            this.grammar.bind(this, ps);
            if (logQueries) {
                logger.debug("Executing INSERT (with generated): " + ps);
            }
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long generated = rs.getLong(1);
                    if (logQueries) {
                        logger.debug("\t - Returned generated value " + generated);
                    }
                    return generated;
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Inserts many elements into the database at a time
     *
     * @param data The data to insert
     *
     * @return The amount of items that were inserted
     */
    public int insertBulk(List<List<Pair>> data) {
        return insertBulk(data, false).size();
    }

    public List<Long> insertBulkWithGenerated(List<List<Pair>> data) {
        return insertBulk(data, true);
    }

    /**
     * Inserts many elements into the database at a time
     *
     * @param data          The data to insert
     * @param withGenerated If generated values should be returned
     *
     * @return A list of generated ids, or a list of nulls
     */
    private List<Long> insertBulk(List<List<Pair>> data, boolean withGenerated) {
        if (data.size() == 0) {
            throw new IllegalArgumentException("Attempting to insert nothing!");
        }
        // Verify the data is valid
        int colCount = data.get(0).size();
        for (List<Pair> row : data) {
            if (row.size() != colCount) {
                throw new IllegalArgumentException(
                    "Inconsistent column count. Expected " + colCount + " got " + row.size());
            }
        }
        data.stream().flatMap(Collection::stream).map(Pair::getValue)
            .forEach(d -> addBinding("insert", d));
        String query = this.grammar.compileInsertMany(this, data);
        try (Connection con = connectionFactory.getConnection();
            PreparedStatement ps = con.prepareStatement(query,
                withGenerated ? Statement.RETURN_GENERATED_KEYS : Statement.NO_GENERATED_KEYS)) {
            this.grammar.bind(this, ps);
            if (logQueries) {
                logger
                    .debug("Executing BULK INSERT (With generated: " + withGenerated + "): " + ps);
            }
            List<Long> generated = new ArrayList<>();
            ps.executeLargeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                while (rs.next()) {
                    generated.add(rs.getLong(1));
                }
                return generated;
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>(data.size());
    }


    /**
     * Adds a join statement to the query
     *
     * @param type     The type of join (INNER, OUTER, LEFT, RIGHT)
     * @param table    The table to join
     * @param first    The first column to join
     * @param operator The operator to compare the columns
     * @param second   The second column to join
     *
     * @return The query builder
     */
    public QueryBuilder join(Type type, String table, String first, String operator,
        String second) {
        if (!Arrays.asList(operators).contains(operator)) {
            throw new IllegalArgumentException("The operator '" + operator + "' was not found!");
        }
        this.joins.add(new JoinElement(table, first, operator, second, type));
        return this;
    }

    /**
     * Adds a <code>LEFT JOIN</code> to the query
     *
     * @param table    The table to join
     * @param first    The first column to join
     * @param operator The operator to compare the columns
     * @param second   The second column to join
     *
     * @return The query builder
     */
    public QueryBuilder leftJoin(String table, String first, String operator, String second) {
        return this.join(Type.LEFT, table, first, operator, second);
    }

    /**
     * Adds a <code>RIGHT JOIN</code> to the query
     *
     * @param table    The table to join
     * @param first    The first column to join
     * @param operator The operator to compare the columns
     * @param second   The second column to join
     *
     * @return The query builder
     */
    public QueryBuilder rightJoin(String table, String first, String operator, String second) {
        return this.join(Type.RIGHT, table, first, operator, second);
    }

    /**
     * Adds a <code>FULL OUTER JOIN</code> to the query
     *
     * @param table    The table to join
     * @param first    The first column to join
     * @param operator The operator to compare the columns
     * @param second   The second column to join
     *
     * @return The query builder
     */
    public QueryBuilder outerJoin(String table, String first, String operator, String second) {
        return this.join(Type.OUTER, table, first, operator, second);
    }

    /**
     * Adds an <code>INNER JOIN</code> to the query
     *
     * @param table    The table to join
     * @param first    The first column to join
     * @param operator The operator to compare the columns
     * @param second   The second column to join
     *
     * @return The query builder
     */
    public QueryBuilder innerJoin(String table, String first, String operator, String second) {
        return this.join(Type.INNER, table, first, operator, second);
    }

    /**
     * Makes the query only return distinct values
     *
     * @return The query builder
     */
    public QueryBuilder distinct() {
        this.distinct = true;
        return this;
    }

    /**
     * Converts the query to a <code>SELECT</code> statement
     *
     * @return The select statement
     */
    public String toSql() {
        return this.grammar.compileSelect(this);
    }

    /**
     * Gets the table that is the target of this query
     *
     * @return The table
     */
    public String getTable() {
        return table;
    }

    /**
     * Gets a list of where clauses on this query
     *
     * @return A list of where elements
     */
    public ArrayList<WhereElement> getWheres() {
        return wheres;
    }

    /**
     * Gets a list of columns to return
     *
     * @return The columns to return
     */
    public String[] getColumns() {
        return columns;
    }

    /**
     * Gets the current grammar used by this query
     *
     * @return The query's grammar
     */
    public Grammar getGrammar() {
        return grammar;
    }

    /**
     * Gets the <code>ORDER BY</code> clauses associated with this query
     *
     * @return The order clause
     */
    public ArrayList<OrderElement> getOrders() {
        return orders;
    }

    /**
     * Parses a {@link ResultSet} into a collection of DbRows
     *
     * @param rs The Result Set
     *
     * @return The result set parsed as a list of {@link DbRow}s
     *
     * @throws SQLException If an exception occurrs
     */
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
