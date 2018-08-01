package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.model.scopes.SoftDeletingScope;

import java.sql.Timestamp;
import java.time.Instant;

public class SoftDeletingModel extends Model {

    @Column("deleted_at")
    public Timestamp deletedAt;

    private transient boolean force = false;

    public SoftDeletingModel() {
        addScope(new SoftDeletingScope(), SoftDeletingScope.SCOPE_NAME);
    }

    public static <T extends SoftDeletingModel> ModelQueryBuilder<T> withTrashed(
        Class<T> modelClass) {
        ModelQueryBuilder<T> q = ModelUtils.getQueryBuilderWithScopes(modelClass);
        q.withoutScope(SoftDeletingScope.SCOPE_NAME);
        return q;
    }

    @Override
    public void delete() {
        if (!force) {
            this.updateTimestamps();
            if (!isDirty("deleted_at")) {
                setData("deleted_at", Timestamp.from(Instant.now()));
            }
            this.save();
        } else {
            super.delete();
        }
    }

    /**
     * Force deletes a model
     */
    public void forceDelete() {
        force = true;
        delete();
        force = false;
    }

    /**
     * Restores a soft deleted model
     */
    public void restore() {
        setData("deleted_at", null);
        save();
    }
}
