package com.mrkirby153.bfs.model.scopes;

import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.sql.QueryBuilder;

/**
 * A transformation applied to all queries on a model
 */
public interface Scope<T extends Model> {

    void apply(QueryBuilder builder, T model);
}
