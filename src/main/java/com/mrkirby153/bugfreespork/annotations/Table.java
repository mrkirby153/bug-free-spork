package com.mrkirby153.bugfreespork.annotations;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

/**
 * Designates the table that this model represents
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Table {
    String value();
}

