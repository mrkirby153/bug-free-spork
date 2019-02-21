package com.mrkirby153.bfs.model.relationships;

import com.mrkirby153.bfs.model.Model;

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


    public Object getParentIdentifier() {
        return parent.getColumnData().get(this.parentKey);
    }

    public abstract void load();

    public V get() {
        if (!this.loaded) {
            this.load();
        }
        return this.value;
    }
}
