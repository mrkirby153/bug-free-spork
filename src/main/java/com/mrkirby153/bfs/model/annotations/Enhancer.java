package com.mrkirby153.bfs.model.annotations;

import java.lang.annotation.Inherited;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Repeatable(Enhancers.class)
@Retention(RetentionPolicy.RUNTIME)
@Inherited
public @interface Enhancer {

    Class<? extends com.mrkirby153.bfs.model.Enhancer> value();
}
