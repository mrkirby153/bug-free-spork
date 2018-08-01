package com.mrkirby153.bfs.sql.elements;

/**
 * A <code>WHERE</code> element in the query
 */
public class WhereElement {

    private String operation;
    private String field;
    private Object object;
    private String bool;

    public WhereElement(String operation, String field, Object object, String bool) {
        this.operation = operation;
        this.field = field;
        this.object = object;
        this.bool = bool;
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

    public String getBool() {

        return bool;
    }
}
