package com.mrkirby153.bfs.sql.elements;

/**
 * Represents a pair of objects
 */
public class Pair {

    private String column;
    private Object value;

    public Pair(String column, Object value) {
        this.column = column;
        this.value = value;
    }

    /**
     * The column
     *
     * @return The column
     */
    public String getColumn() {
        return column;
    }

    /**
     * The value of the object
     *
     * @return The value
     */
    public Object getValue() {
        return value;
    }
}
