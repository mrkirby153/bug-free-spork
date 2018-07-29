package com.mrkirby153.bfs.model.traits;

public interface HasTimestamps {

    default String getCreatedAt(){
        return "created_at";
    }

    default String getUpdatedAt(){
        return "updated_at";
    }
}
