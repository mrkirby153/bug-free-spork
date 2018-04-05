package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.sql.elements.GenericElement;
import com.mrkirby153.bfs.sql.elements.OrderElement;
import com.mrkirby153.bfs.sql.grammars.Grammar;
import com.mrkirby153.bfs.sql.grammars.MySqlGrammar;

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

    public QueryBuilder(){
        this.grammar = new MySqlGrammar();
    }

    public QueryBuilder select(String... columns) {
        this.columns = columns;
        return this;
    }

    public QueryBuilder from(String table) {
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

    public QueryBuilder orderBy(String column, String direction){
        this.orders.add(new OrderElement(column, direction));
        return this;
    }

    public String toSql(){
        return this.grammar.compileSelect(this);
    }

    public static String[] getOperators() {
        return operators;
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
