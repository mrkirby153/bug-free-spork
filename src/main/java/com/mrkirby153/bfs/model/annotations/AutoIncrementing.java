package com.mrkirby153.bfs.model.annotations;

import com.mrkirby153.bfs.model.Constants;

import java.lang.annotation.ElementType;
import java.lang.annotation.Target;

/**
 * Annotation determining if the model is auto-incrementing
 */
@Target(ElementType.TYPE)
public @interface AutoIncrementing {

    /**
     * If the table is auto incrementing
     *
     * @return The table's auto increment status
     */
    boolean value() default true;

    /**
     * The column that auto increments. Defaults to the model's primary key
     *
     * @return The column name that auto increments
     */
    String column() default Constants.PRIMARY_KEY_MARKER;
}
