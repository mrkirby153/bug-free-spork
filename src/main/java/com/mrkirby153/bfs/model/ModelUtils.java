package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.model.annotations.Column;
import com.mrkirby153.bfs.model.annotations.Table;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class ModelUtils {

    private static final Map<Class<? extends Model>, String> tableCache = new ConcurrentHashMap<>();

    /**
     * Gets the table of the provided {@link Model}. The table name is set with the {@link Table @Table} annotation
     *
     * @param clazz The mode class
     *
     * @return The table name
     */
    public static <T extends Model> String getTable(Class<T> clazz) {
        return tableCache.computeIfAbsent(clazz, c -> {
            if (c.isAnnotationPresent(Table.class)) {
                return c.getAnnotation(Table.class).value();
            } else {
                return null;
            }
        });
    }

    public static String getColumnName(Field field) {
        return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).value()
            : field.getName();
    }
}
