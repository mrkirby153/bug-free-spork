package com.mrkirby153.bfs.sql.elements;

/**
 * An order element on the query
 */
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

    /**
     * Gets the column
     *
     * @return The column
     */
    public String getColumn() {
        return column;
    }

    /**
     * Gets the direction to order
     *
     * @return The direction
     */
    public String getDirection() {
        return direction;
    }
}
