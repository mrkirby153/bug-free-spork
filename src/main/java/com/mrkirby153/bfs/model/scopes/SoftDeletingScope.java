package com.mrkirby153.bfs.model.scopes;

import com.mrkirby153.bfs.model.SoftDeletingModel;
import com.mrkirby153.bfs.sql.QueryBuilder;

public class SoftDeletingScope implements Scope<SoftDeletingModel> {

    public static final String SCOPE_NAME = "soft_deleting";

    @Override
    public void apply(QueryBuilder builder, SoftDeletingModel model) {
        builder.whereNull("deleted_at");
    }
}
