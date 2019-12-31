package com.mrkirby153.bfs.model;

/**
 * An enhancer for model queries
 */
public interface Enhancer {

    /**
     * Called when a query builder is constructed
     *
     * @param builder The builder
     */
    default void enhance(ModelQueryBuilder<? extends Model> builder) {

    }

    /**
     * Called before the querying of a model
     *
     * @param builder The builder
     */
    default void onQuery(ModelQueryBuilder<? extends Model> builder) {

    }

    /**
     * Called before insert
     *
     * @param model   The model that will be inserted
     * @param builder The query builder that will be used
     */
    default void onInsert(Model model, ModelQueryBuilder<? extends Model> builder) {

    }

    /**
     * Called before update
     *
     * @param model   The model that will be updated
     * @param builder The query builder that will be used
     */
    default void onUpdate(Model model, ModelQueryBuilder<? extends Model> builder) {

    }

    /**
     * Called before delete
     *
     * @param model   The model that will be deleted
     * @param builder The builder that will be used
     */
    default void onDelete(Model model, ModelQueryBuilder<? extends Model> builder) {

    }

    /**
     * The name of the enhancer
     *
     * @return The name of the enhancer
     */
    String name();
}
