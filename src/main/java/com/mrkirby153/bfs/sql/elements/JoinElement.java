package com.mrkirby153.bfs.sql.elements;

public class JoinElement {

    private String table, first, operation, second;
    private Type type;

    public JoinElement(String table, String first, String operation, String second, Type type) {
        this.table = table;
        this.first = first;
        this.second = second;
        this.operation = operation;
        this.type = type;
    }

    public String getSql() {
        return this.type.toString() + " JOIN " + this.table + " ON " + this.first + " "
            + this.operation + " " + this.second;
    }

    public enum Type {
        INNER("INNER"),
        OUTER("FULL OUTER"),
        LEFT("LEFT"),
        RIGHT("RIGHT");

        private String type;

        Type(String type) {
            this.type = type;
        }

        @Override
        public String toString() {
            return this.type;
        }
    }
}
