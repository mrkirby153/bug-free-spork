package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.annotations.ApplyScopes;
import com.mrkirby153.bfs.annotations.Table;
import com.mrkirby153.bfs.model.scopes.Scope;
import com.mrkirby153.bfs.model.scopes.ScopeUtils;

import java.util.HashMap;

public class ModelUtils {

    private static HashMap<Class<? extends Model>, String> tableCache = new HashMap<>();
    private static HashMap<Class<? extends Model>, Class<? extends Scope>[]> scopeCache = new HashMap<>();

    /**
     * Gets the table of the provided {@link Model}. The table name is set with the {@link Table @Table} annotation
     *
     * @param clazz The mode class
     *
     * @return The table name
     */
    protected static <T extends Model> String getTable(Class<T> clazz) {
        return tableCache.computeIfAbsent(clazz, c -> {
            if (c.isAnnotationPresent(Table.class)) {
                return c.getAnnotation(Table.class).value();
            } else {
                return null;
            }
        });
    }

    /**
     * Gets the scopes that should be applied to the {@link Model}
     *
     * @param clazz The model class
     *
     * @return An array of scopes or null if there are no scopes registered.
     */
    protected static <T extends Model> Class<? extends Scope>[] getScopes(Class<T> clazz) {
        return scopeCache.computeIfAbsent(clazz, c -> {
            if (c.isAnnotationPresent(ApplyScopes.class)) {
                return c.getAnnotation(ApplyScopes.class).value();
            } else {
                return null;
            }
        });
    }

    /**
     * Returns a {@link ModelQueryBuilder} with the model's scopes applied to it
     *
     * @param clazz The class of the model
     *
     * @return A query builder for the model
     */
    @SuppressWarnings("unchecked")
    protected static <T extends Model> ModelQueryBuilder<T> getQueryBuilderWithScopes(
        Class<T> clazz) {
        ModelQueryBuilder<T> qb = new ModelQueryBuilder<>(Model.getDefaultGrammar(), clazz);
        Class<? extends Scope>[] scopes = getScopes(clazz);
        if(scopes != null) {
            for (Class<? extends Scope> scope : scopes) {
                qb.addScope(scope);
                ScopeUtils.getScope(scope).extend(qb);
            }
        }
        return qb;
    }

    /**
     * Returns a {@link ModelQueryBuilder} without scopes applied to it
     *
     * @param clazz The class of the model
     *
     * @return A query builder for the model
     */
    @SuppressWarnings("unchecked")
    protected static <T extends Model> ModelQueryBuilder<T> getQueryBuilderWithoutScopes(
        Class<T> clazz) {
        return new ModelQueryBuilder<>(Model.getDefaultGrammar(), clazz);
    }
}
