package com.mrkirby153.bfs.sql;

import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.sql.elements.WhereElement;

import java.util.ArrayList;
import java.util.List;

/**
 * A query builder for translating model operations into SQL queries
 */
public class QueryBuilder<T extends Model> {

    private Class<T> modelClass;

    private T modelInstance;

    private List<QueryElement> queryElements = new ArrayList<>();
    private List<WhereElement> scopes = new ArrayList<>();

    public QueryBuilder(Class<T> clazz) {
        this.modelClass = clazz;
    }

    public QueryBuilder(T instance) {
        this.modelInstance = instance;
        this.modelClass = (Class<T>) instance.getClass();
    }
}
