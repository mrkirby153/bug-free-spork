package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.ConnectionFactory;
import com.mrkirby153.bfs.sql.elements.JoinElement;
import com.mrkirby153.bfs.sql.elements.JoinElement.Type;
import com.mrkirby153.bfs.sql.elements.OrderElement;
import com.mrkirby153.bfs.sql.elements.Pair;
import com.mrkirby153.bfs.sql.elements.WhereElement;
import com.mrkirby153.bfs.sql.grammars.Grammar;
import com.mrkirby153.bfs.sql.grammars.MySqlGrammar;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * A query builder for translating model operations into SQL queries
 */
public class QueryBuilder {

    private static final String[] operators = new String[]{
        "=", "<", ">", "<=", ">=", "<>", "!=", "<=>", "like", "like binary", "not like", "ilike",
        "&", "|", "^", "<<", ">>", "rlike", "regexp", "not regexp", "~", "~*", "!~*", "similar to",
        "not similar to", "not ilike", "~~*", "!~~*"
    };
    public static ConnectionFactory connectionFactory = null;
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

    /**
     * Adds a <code>WHERE</code> clause to the query
     *
     * @param column   The column to use in the clause
     * @param operator The operator to use when comparing
     * @param value    The value to compare
     *
     * @return The query builder
     *
     * @throws IllegalArgumentException If the operator provided is not a valid operator
     */
    public QueryBuilder where(String column, String operator, Object value) {
        if (!Arrays.asList(operators).contains(operator.toLowerCase())) {
            throw new IllegalArgumentException("The operator " + operator + " is not valid!");
        }
        WhereElement e = new WhereElement(operator, column, value);
        this.wheres.add(e);
        return this;
    }

    /**
     * Adds a <code>WHERE</code> clause using the equality operator
     *
     * @param column The column to check
     * @param value  The value
     *
     * @return The query builder
     */
    public QueryBuilder where(String column, Object value) {
        return this.where(column, "=", value);
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
        String query = this.grammar.compileUpdate(this, data);
        try (Connection con = connectionFactory.getConnection()) {
            PreparedStatement statement = con.prepareStatement(query);
            grammar.bindUpdate(this, statement, data);
            return statement.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    /**
     * Executes the query and returns a {@link ResultSet}
     *
     * @return The {@link ResultSet} generated by the query
     */
    public ResultSet get() {
        String query = this.grammar.compileSelect(this);
        Connection con = connectionFactory.getConnection();
        PreparedStatement statement = null;
        try {
            statement = con.prepareStatement(query);
            grammar.bindSelect(this, statement);
            return statement.executeQuery();
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * Executes a <code>DELETE</code> operation on the query
     *
     * @return True if the delete was successful
     */
    public boolean delete() {
        String query = this.grammar.compileDelete(this);
        try (Connection con = connectionFactory.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query);
            this.grammar.bindDelete(this, ps);
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
        try (Connection con = connectionFactory.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query);
            this.grammar.bindExists(this, ps);

            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getBoolean("exists");
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
        String query = this.grammar.compileInsert(this, data);
        try (Connection con = connectionFactory.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query);
            this.grammar.bindInsert(this, ps, data);
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
        String query = this.grammar.compileInsert(this, data);
        try (Connection con = connectionFactory.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            this.grammar.bindInsert(this, ps, data);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getLong(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
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
}
