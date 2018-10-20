package com.mrkirby153.bfs.annotations;

import com.mrkirby153.bfs.model.scopes.Scope;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation denoting scopes that should be applied to the model
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Inherited
public @interface ApplyScopes {

    /**
     * Returns an array of the {@link com.mrkirby153.bfs.model.scopes.Scope Scopes} that should be
     * applied to the model when querying
     *
     * @return An array of classes
     */
    Class<? extends Scope>[] value() default {};
}
