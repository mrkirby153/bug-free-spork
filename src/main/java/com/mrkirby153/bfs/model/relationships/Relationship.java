package com.mrkirby153.bfs.model.relationships;

import com.mrkirby153.bfs.model.Model;

/**
 * Represents an abstract relationship
 *
 * @param <T> The model
 * @param <V> The type of return value from the {@link Relationship#get()} method
 */
public abstract class Relationship<T extends Model, V> {

    protected String parentKey;
    protected String localKey;

    protected Model parent;
    protected Class<T> referenced;

    protected boolean loaded = false;
    protected V value;

    public Relationship(Model parent, Class<T> referenced, String parentKey, String localKey) {
        this.parentKey = parentKey;
        this.localKey = localKey;
        this.parent = parent;
        this.referenced = referenced;
    }


    /**
     * Gets the value of the foreign key
     *
     * @return The value
     */
    public Object getParentIdentifier() {
        return parent.getColumnData().get(this.parentKey);
    }

    /**
     * Loads the foreign key
     */
    public abstract void load();

    /**
     * Gets the value of the foreign key
     *
     * @return The value of the key
     */
    public V get() {
        if (!this.loaded) {
            this.load();
        }
        return this.value;
    }
}
