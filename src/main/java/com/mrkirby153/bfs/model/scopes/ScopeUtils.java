package com.mrkirby153.bfs.model.scopes;

import java.util.HashMap;

/**
 * Utility class for translating Scope classes to Scope objects.
 *
 * Scope instances are cached to reduce overhead and aren't guaranteed to be unique
 */
public class ScopeUtils {

    /**
     * A cache of scope classes to scope instances.
     */
    private static HashMap<Class<? extends Scope>, Scope> scopeCache = new HashMap<>();

    /**
     * Gets an instance of the scope class
     *
     * @param clazz The scope class
     *
     * @return The instance of the class
     */
    public static Scope getScope(Class<? extends Scope> clazz) {
        return scopeCache.computeIfAbsent(clazz, c -> {
            try {
                return c.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return null;
        });
    }

    /**
     * Gets the identifier of the given scope
     *
     * @param clazz The scope
     *
     * @return The scope's ID
     */
    public static String getIdentifier(Class<? extends Scope> clazz) {
        return getScope(clazz).identifier();
    }

    /**
     * Clears the internal scope cache.
     *
     * <b>This probably will never need to be called</b>
     */
    public static void clearScopeCache() {
        scopeCache.clear();
    }
}
