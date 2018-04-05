package com.mrkirby153.bfs.sql;

public enum Binding {
    SELECT("SELECT"),
    FROM("FROM"),
    JOIN(""),
    WHERE("AND WHERE"),
    ORDER("ORDER BY");

    private String prefix;

    Binding(String prefix) {
        this.prefix = prefix;
    }

    public String getPrefix() {
        return prefix;
    }
}
