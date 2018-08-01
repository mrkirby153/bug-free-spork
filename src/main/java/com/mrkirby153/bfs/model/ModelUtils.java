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

    @SuppressWarnings("unchecked")
    protected static <T extends Model> ModelQueryBuilder<T> getQueryBuilderWithScopes(
        Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            ModelQueryBuilder<T> qb = new ModelQueryBuilder<>(Model.getDefaultGrammar(), clazz);
            qb.table(instance.getTable());
            qb.addScopes(instance.getScopes());
            return qb;
        } catch (InstantiationException | IllegalAccessException e) {
           throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unchecked")
    protected static <T extends Model> ModelQueryBuilder<T> getQueryBuilderWithoutScopes(
        Class<T> clazz) {
        try {
            T instance = clazz.newInstance();
            ModelQueryBuilder<T> qb = new ModelQueryBuilder<>(Model.getDefaultGrammar(), clazz);
            qb.table(instance.getTable());
            return qb;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
