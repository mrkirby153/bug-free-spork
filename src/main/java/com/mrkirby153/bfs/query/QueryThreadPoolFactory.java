package com.mrkirby153.bfs.query;

import java.util.concurrent.ThreadFactory;

/**
 * Thread pool factory for query threads
 */
class QueryThreadPoolFactory implements ThreadFactory {

    private int threadNumber = 1;

    @Override
    public Thread newThread(Runnable r) {
        return new Thread(r, String.format("bfs-query-%d", threadNumber++));
    }
}
