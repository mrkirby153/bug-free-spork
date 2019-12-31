package com.mrkirby153.bfs.query.event;

import com.mrkirby153.bfs.query.QueryBuilder;
import com.mrkirby153.bfs.query.event.QueryEvent.Type;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;

@Slf4j
public class QueryEventManager {

    /**
     * Calls events on a query builder
     *
     * @param type    The type of event to call
     * @param builder The builder to call events on
     *
     * @return True if the execution should be halted
     */
    public static boolean callEvents(Type type, QueryBuilder builder) {
        log.trace("Calling {} events on {}", type, builder);
        List<QueryEventListener> listeners = builder.getEventListeners()
            .getOrDefault(type, Collections.emptyList());
        QueryEvent event = new QueryEvent(type, builder);
        listeners.forEach(queryEventListener -> {
            try {
                queryEventListener.onEvent(event);
            } catch (Exception e) {
                log.error("A query event listener threw an exception", e);
            }
        });
        return event.isCanceled();
    }
}
