package com.mrkirby153.bfs.sql.elements;

public class OrderElement {

    private String column;

    private String direction;

    public OrderElement(String column, String direction) {
        this.column = column;
        this.direction = direction;
        if (!direction.equalsIgnoreCase("asc") && !direction.equalsIgnoreCase("desc")) {
            throw new IllegalArgumentException("Invalid direction for ordering");
        }
    }

    public String getColumn() {
        return column;
    }

    public String getDirection() {
        return direction;
    }
}
