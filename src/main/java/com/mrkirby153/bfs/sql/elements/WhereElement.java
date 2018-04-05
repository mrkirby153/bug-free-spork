package com.mrkirby153.bfs.sql.elements;

import com.mrkirby153.bfs.sql.QueryElement;

import java.util.Collections;
import java.util.List;

/**
 * A <code>WHERE</code> element in the query
 */
public class WhereElement implements QueryElement {

    private String operation;
    private String field;
    private Object object;

    public WhereElement(String operation, String field, Object object) {
        this.operation = operation;
        this.field = field;
        this.object = object;
    }

    @Override
    public String getQuery() {
        return String.format("`%s` %s ?", this.field, this.operation);
    }

    @Override
    public List<Object> getBindings() {
        return Collections.singletonList(this.object);
    }

    @Override
    public int priority() {
        return  0;
    }
}
