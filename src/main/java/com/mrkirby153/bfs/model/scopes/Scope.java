package com.mrkirby153.bfs.model.scopes;

import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.model.ModelQueryBuilder;
import com.mrkirby153.bfs.sql.QueryBuilder;

/**
 * A Scope is a modification to <code>SELECT</code> statements
 * to provide additional constraints`
 */
public interface Scope<T extends Model> {

    /**
     * Apply the scope to the {@link QueryBuilder}
     *
     * @param instance The instance of the model to apply the scopes on, if any
     * @param builder  The query builder
     */
    void apply(T instance, QueryBuilder builder);

    /**
     * Extends the query builder
     *
     * @param builder The query builder to extend
     */
    void extend(ModelQueryBuilder<T> builder);

    /**
     * Returns the identifier of the scope
     *
     * @return The scope's identifier.
     */
    default String identifier() {
        return this.getClass().getSimpleName().toLowerCase();
    }

}
