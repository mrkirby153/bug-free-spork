package com.mrkirby153.bfs.sql.elements;

/**
 * A <code>WHERE</code> element in the query
 */
public class WhereElement {

    private String operation;
    private String field;
    private Object object;

    public WhereElement(String operation, String field, Object object) {
        this.operation = operation;
        this.field = field;
        this.object = object;
    }

    public String getOperation() {
        return operation;
    }

    public String getField() {
        return field;
    }

    public Object getBinding(){
        return this.object;
    }
}
