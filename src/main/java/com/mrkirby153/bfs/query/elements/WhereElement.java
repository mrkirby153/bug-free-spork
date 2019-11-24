package com.mrkirby153.bfs.query.elements;

import com.mrkirby153.bfs.Pair;
import lombok.Getter;
import lombok.NonNull;

import java.util.HashMap;
import java.util.Map;

/**
 * A {@code WHERE} element in the query
 */
@Getter
public class WhereElement {

    /**
     * The type of query
     */
    @NonNull
    private final Type type;

    private final String column;
    private final String bool;

    private Map<String, Object> data = new HashMap<>();

    public WhereElement(Type type, String column, String bool, Pair<String, Object>... data) {
        this.type = type;
        this.column = column;
        this.bool = bool;
        for (Pair<String, Object> p : data) {
            this.data.put(p.getFirst(), p.getSecond());
        }
    }

    public Object get(String key) {
        return data.get(key);
    }

    public enum Type {
        BASIC,
        NOT_NULL,
        NULL,
        IN,
        NOT_IN,
        SUB,
        NOT_SUB
    }
}
