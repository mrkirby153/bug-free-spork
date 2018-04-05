package com.mrkirby153.bfs.sql.elements;

public class Pair {

    private String column;
    private Object value;

    public Pair(String column, Object value) {
        this.column = column;
        this.value = value;
    }

    public String getColumn() {
        return column;
    }

    public Object getValue() {
        return value;
    }
}
