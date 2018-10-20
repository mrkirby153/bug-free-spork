package com.mrkirby153.bfs.model.scopes;

import com.mrkirby153.bfs.model.ModelQueryBuilder;
import com.mrkirby153.bfs.model.SoftDeletingModel;
import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.Pair;

import java.sql.Timestamp;
import java.time.Instant;

public class SoftDeletingScope implements Scope<SoftDeletingModel> {

    public static String SCOPE_NAME = "soft_deletes";

    @Override
    public void apply(SoftDeletingModel instance, QueryBuilder builder) {
        builder.whereNull("deleted_at");
    }

    @Override
    public void extend(ModelQueryBuilder<SoftDeletingModel> builder) {
        builder.onDelete(b -> {
            b.update(new Pair("deleted_at", Timestamp.from(Instant.now())));
            return true;
        });
    }


    @Override
    public String identifier() {
        return SCOPE_NAME;
    }
}
