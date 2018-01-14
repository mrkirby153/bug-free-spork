package com.mrkirby153.bugfreespork.model;


/**
 * A SQL clause representing a "WHERE" statement
 */
class WhereClause {

    private String column;
    private String test;
    private String value;

    public WhereClause(String column, String test, String value) {
        this.column = column;
        this.test = test;
        this.value = value;
    }

    /**
     * Gets the column of the statement
     *
     * @return The column
     */
    public String getColumn() {
        return column;
    }

    /**
     * Gets the logical test to use for this
     *
     * @return The Test to use
     */
    public String getTest() {
        return test;
    }

    /**
     * Gets the value of the column
     *
     * @return The value of the column
     */
    public String getValue() {
        return value;
    }
}