package com.mrkirby153.bfs.sql.elements;

import com.mrkirby153.bfs.Tuple;

import java.util.HashMap;

/**
 * A <code>WHERE</code> element in the query
 */
public class WhereElement {

    private String type;

    private HashMap<String, Object> data = new HashMap<>();

    public WhereElement(String type, Tuple<String, Object>... data) {
        this.type = type;
        for (Tuple<String, Object> t : data) {
            this.data.put(t.first, t.second);
        }
    }

    public Object get(String key) {
        return data.get(key);
    }

    public String getType() {
        return type;
    }
}
