package com.mrkirby153.bfs.model.relationships;

import com.mrkirby153.bfs.model.Model;

import java.util.List;

public class OneToManyRelationship<T extends Model> extends Relationship<T, List<T>> {

    public OneToManyRelationship(Model parent, Class<T> referenced, String parentKey,
        String localKey) {
        super(parent, referenced, parentKey, localKey);
    }

    @Override
    public void load() {
        if (this.loaded) {
            return;
        }
        this.value = Model.query(this.referenced).where(this.localKey, getParentIdentifier())
            .get();
        this.loaded = true;
    }
}
