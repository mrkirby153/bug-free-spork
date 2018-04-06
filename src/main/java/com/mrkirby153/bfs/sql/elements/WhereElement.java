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

    public String query(){
        return String.format("`%s` %s ?", this.field, this.operation);
    }

    public Object getBinding(){
        return this.object;
    }
}
