package com.mrkirby153.bfs;

import lombok.Data;

/**
 * Represents a pair of objects
 *
 * @param <A> The type of the first object
 * @param <B> The type of second object
 */
@Data
public class Pair<A, B> {

    private final A first;
    private final B second;
}
