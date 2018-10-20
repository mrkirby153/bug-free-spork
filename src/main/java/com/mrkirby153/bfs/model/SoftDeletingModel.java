package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.annotations.ApplyScopes;
import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.model.scopes.SoftDeletingScope;

import java.sql.Timestamp;
import java.time.Instant;

@ApplyScopes(SoftDeletingScope.class)
public class SoftDeletingModel extends Model {

    @Column("deleted_at")
    public Timestamp deletedAt;

    private transient boolean force = false;

    /**
     * Returns a query builder that includes trashed models
     *
     * @param modelClass A model
     *
     * @return The query builder
     */
    public static <T extends SoftDeletingModel> ModelQueryBuilder<T> withTrashed(
        Class<T> modelClass) {
        return ModelUtils.getQueryBuilderWithScopes(modelClass)
            .withoutScope(SoftDeletingScope.SCOPE_NAME);
    }

    /**
     * Returns a query builder that only operates on trashed models
     *
     * @param modelClass The model class
     *
     * @return The query builder
     */
    public static <T extends SoftDeletingModel> ModelQueryBuilder<T> trashed(Class<T> modelClass) {
        return ModelUtils.getQueryBuilderWithScopes(modelClass)
            .withoutScope(SoftDeletingScope.SCOPE_NAME).whereNotNull("deleted_at");
    }

    @Override
    public void delete() {
        if (force) {
            super.delete();
        } else {
            this.updateTimestamps();
            if (!isDirty("deleted_at")) {
                setData("deleted_at", Timestamp.from(Instant.now()));
            }
            this.save();
        }
    }

    /**
     * Force delete the model from the database.
     */
    public void forceDelete() {
        force = true;
        this.delete();
        force = false;
    }

    /**
     * Restores a soft-deleted model
     */
    public void restore() {
        setData("deleted_at", null);
        this.save();
    }

    /**
     * Returns if the mode is trashed (Soft-deleted, but still exists in the database)
     *
     * @return True if the model is trashed
     */
    public boolean isTrashed() {
        return this.deletedAt != null;
    }
}
