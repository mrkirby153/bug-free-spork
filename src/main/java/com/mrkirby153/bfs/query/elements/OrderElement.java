package com.mrkirby153.bfs.query.elements;

import lombok.Getter;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;

/**
 * An {@code ORDER} element in the query
 */
@RequiredArgsConstructor
@Getter
public class OrderElement {

    /**
     * The column to order by
     */
    @NonNull
    private final String column;

    /**
     * The direction of the ordering
     */
    private final Direction direction;

    public enum Direction {
        ASC,
        DESC
    }
}
