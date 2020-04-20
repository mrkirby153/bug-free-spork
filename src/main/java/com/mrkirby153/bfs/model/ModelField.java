package com.mrkirby153.bfs.model;


import lombok.Data;

import java.lang.reflect.Field;

/**
 * Data class storing a model's field for caching.
 */
@Data
class ModelField {

    /**
     * The name of the field
     */
    private final String name;

    /**
     * The field itself
     */
    private final Field field;
}
