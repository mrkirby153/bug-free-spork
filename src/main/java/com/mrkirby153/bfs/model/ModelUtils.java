package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.annotations.Table;

import java.util.HashMap;

public class ModelUtils {

    private static HashMap<Class<? extends Model>, String> tableCache = new HashMap<>();

    protected static <T extends Model> String getTable(Class<T> clazz) {
        return tableCache.computeIfAbsent(clazz, c -> {
            if (c.isAnnotationPresent(Table.class)) {
                return c.getAnnotation(Table.class).value();
            } else {
                return null;
            }
        });
    }
}
