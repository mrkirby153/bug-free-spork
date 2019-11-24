package com.mrkirby153.bfs.query.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * A join element in a query
 */
@RequiredArgsConstructor
@Getter
public class JoinElement {

    @NonNull
    private final String table;

    @NonNull
    private final String firstColumn;

    @NonNull
    private final String operation;

    @NonNull
    private final String secondColumn;

    private final Type type;

    public enum Type {
        INNER,
        OUTER,
        LEFT,
        RIGHT
    }
}
