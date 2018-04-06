package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.ConnectionFactory;
import com.mrkirby153.bfs.sql.elements.GenericElement;
import com.mrkirby153.bfs.sql.elements.OrderElement;
import com.mrkirby153.bfs.sql.elements.Pair;
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
    private ArrayList<GenericElement> wheres = new ArrayList<>();

    /**
     * A list of columns to return
     */
    private String[] columns = new String[0];

    private ArrayList<OrderElement> orders = new ArrayList<>();

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

    public QueryBuilder where(String column, String operator, Object value) {
        if (!Arrays.asList(operators).contains(operator.toLowerCase())) {
            throw new IllegalArgumentException("The operator " + operator + " is not valid!");
        }
        GenericElement e = new GenericElement("`" + column + "` " + operator + " ?", value);
        this.wheres.add(e);
        return this;
    }

    public QueryBuilder orderBy(String column, String direction) {
        this.orders.add(new OrderElement(column, direction));
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

    public String toSql() {
        return this.grammar.compileSelect(this);
    }

    public String getTable() {
        return table;
    }

    public ArrayList<GenericElement> getWheres() {
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
