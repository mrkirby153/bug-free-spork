package com.mrkirby153.bfs.sql.elements;

public class WhereNullElemenet extends WhereElement {

    public WhereNullElemenet(String field, String type) {
        super("is", field, type, "AND");
    }
}
