package com.mrkirby153.bfs.query.event;

import com.mrkirby153.bfs.query.QueryBuilder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Event fired in response to various query events
 */
@Getter
@RequiredArgsConstructor
public class QueryEvent {

    /**
     * The type of the event
     */
    private final Type type;

    /**
     * The query builder
     */
    private final QueryBuilder queryBuilder;

    /**
     * If the event is canceled
     */
    private boolean canceled = false;

    public void setCanceled(boolean canceled) {
        if (!type.cancelable && canceled) {
            throw new IllegalStateException("Cannot cancel an uncancelable event");
        }
        this.canceled = canceled;
    }

    @RequiredArgsConstructor
    public enum Type {
        /**
         * Called before the data is inserted
         */
        PRE_CREATE(true),
        /**
         * Called after the data is inserted
         */
        POST_CREATE(false),

        /**
         * Called before the database is updated
         */
        PRE_UPDATE(true),
        /**
         * Called after the database is updated
         */
        POST_UPDATE(false),

        /**
         * Called before rows are deleted
         */
        PRE_DELETE(true),
        /**
         * Called after rows are deleted
         */
        POST_DELETE(false),

        /**
         * Called before rows are queried
         */
        PRE_GET(false),
        /**
         * Called after rows are queried
         */
        POST_GET(false);

        final boolean cancelable;
    }
}
