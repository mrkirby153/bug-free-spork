package com.mrkirby153.bfs.sql.elements;

public class WhereNullElemenet extends WhereElement {

    private boolean not;

    public WhereNullElemenet(String field, boolean not, String bool) {
        super(null, field, "NULL", bool);
        this.not = not;
    }

    public boolean isNot() {
        return not;
    }
}
