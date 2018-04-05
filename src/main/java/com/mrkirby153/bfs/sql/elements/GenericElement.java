package com.mrkirby153.bfs.sql.elements;

import com.mrkirby153.bfs.sql.QueryElement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Represents arbitrary elements in the query
 */
public class GenericElement implements QueryElement {

    private String query;

    private List<Object> objects = new ArrayList<>();

    public GenericElement(String query){
        this.query = query;
    }

    public GenericElement(String query, Object... objects) {
        this.query = query;
        this.objects.addAll(Arrays.asList(objects));
    }


    @Override
    public String getQuery() {
        return this.query;
    }

    @Override
    public List<Object> getBindings() {
        return this.objects;
    }
}
