package com.mrkirby153.bfs.model.annotations;

import com.mrkirby153.bfs.model.Enhancer;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * A list of enhancers to apply to the model
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Enhancers {

    Class<? extends Enhancer>[] value();
}
