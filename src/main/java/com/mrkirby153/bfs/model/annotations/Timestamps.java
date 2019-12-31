package com.mrkirby153.bfs.model.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Alias annotation for including the {@link com.mrkirby153.bfs.model.enhancers.TimestampEnhancer} enhancer
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Timestamps {
}
