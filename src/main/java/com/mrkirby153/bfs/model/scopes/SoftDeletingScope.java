package com.mrkirby153.bfs.model.scopes;

import com.mrkirby153.bfs.model.SoftDeletingModel;
import com.mrkirby153.bfs.sql.QueryBuilder;

public class SoftDeletingScope implements Scope<SoftDeletingModel> {

    public static String SCOPE_NAME = "soft_deletes";
    @Override
    public void apply(SoftDeletingModel instance, QueryBuilder builder) {
        builder.whereNull("deleted_at");
    }

    @Override
    public String identifier() {
        return SCOPE_NAME;
    }
}
