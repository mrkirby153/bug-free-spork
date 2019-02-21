package com.mrkirby153.bfs.model.relationships;

import com.mrkirby153.bfs.model.Model;

/**
 * Represents a one to one relationship
 * @param <T>
 */
public class OneToOneRelationship<T extends Model> extends Relationship<T, T> {

    public OneToOneRelationship(Model parent, Class<T> referenced, String parentKey,
        String localKey) {
        super(parent, referenced, parentKey, localKey);
    }

    @Override
    public void load() {
        if (this.loaded) {
            return;
        }
        this.value = Model.query(this.referenced).where(this.localKey, getParentIdentifier()).first();
        this.loaded = true;
    }
}
