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

    private ArrayList<OrderElement> orders = new ArrayList<>();
    private ArrayList<JoinElement> joins = new ArrayList<>();

    private Long limit = null;

    private Long offset = null;

    private Grammar grammar;

    public QueryBuilder() {
        this.grammar = new MySqlGrammar();
    }

    public static String[] getOperators() {
        return operators;
    }

    public QueryBuilder select(String... columns) {
        this.columns = columns;
        return this;
    }

    public QueryBuilder from(String table) {
        this.table = table;
        return this;
    }

    public QueryBuilder table(String table) {
        this.table = table;
        return this;
    }

    public ArrayList<JoinElement> getJoins() {
        return joins;
    }

    public Long getLimit() {
        return limit;
    }

    public Long getOffset() {
        return offset;
    }

    public QueryBuilder where(String column, String operator, Object value) {
        if (!Arrays.asList(operators).contains(operator.toLowerCase())) {
            throw new IllegalArgumentException("The operator " + operator + " is not valid!");
        }
        WhereElement e = new WhereElement(operator, column, value);
        this.wheres.add(e);
        return this;
    }

    public QueryBuilder orderBy(String column, String direction) {
        this.orders.add(new OrderElement(column, direction));
        return this;
    }

    public QueryBuilder limit(long amount) {
        this.limit = amount;
        return this;
    }

    public QueryBuilder offset(long amount) {
        this.offset = amount;
        return this;
    }

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

    public int insertWithGenerated(Pair... data) {
        String query = this.grammar.compileInsert(this, data);
        try (Connection con = connectionFactory.getConnection()) {
            PreparedStatement ps = con.prepareStatement(query, Statement.RETURN_GENERATED_KEYS);
            this.grammar.bindInsert(this, ps, data);
            ps.executeUpdate();
            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return -1;
    }

    public QueryBuilder join(Type type, String table, String first, String operator, String second) {
        if (!Arrays.asList(operators).contains(operator)) {
            throw new IllegalArgumentException("The operator '" + operator + "' was not found!");
        }
        this.joins.add(new JoinElement(table, first, operator, second, type));
        return this;
    }

    public QueryBuilder leftJoin(String table, String first, String operator, String second) {
        return this.join(Type.LEFT, table, first, operator, second);
    }

    public QueryBuilder rightJoin(String table, String first, String operator, String second) {
        return this.join(Type.RIGHT, table, first, operator, second);
    }

    public QueryBuilder outerJoin(String table, String first, String operator, String second) {
        return this.join(Type.OUTER, table, first, operator, second);
    }

    public QueryBuilder innerJoin(String table, String first, String operator, String second) {
        return this.join(Type.INNER, table, first, operator, second);
    }

    public String toSql() {
        return this.grammar.compileSelect(this);
    }

    public String getTable() {
        return table;
    }

    public ArrayList<WhereElement> getWheres() {
        return wheres;
    }

    public String[] getColumns() {
        return columns;
    }

    public Grammar getGrammar() {
        return grammar;
    }

    public ArrayList<OrderElement> getOrders() {
        return orders;
    }
}
