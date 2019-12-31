package com.mrkirby153.bfs.model.enhancers;

import com.mrkirby153.bfs.model.Constants;
import com.mrkirby153.bfs.model.Enhancer;
import com.mrkirby153.bfs.model.Model;
import com.mrkirby153.bfs.model.ModelQueryBuilder;
import com.mrkirby153.bfs.model.ModelUtils;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Enhancer responsible for updating created at and updated at timestamp fields
 */
@Slf4j
public class TimestampEnhancer implements Enhancer {

    private static Set<Class<? extends Model>> cachedModels = new HashSet<>();
    private static Map<Class<? extends Model>, List<String>> createdTimestampColCache = new HashMap<>();
    private static Map<Class<? extends Model>, List<String>> updatedTimestampColCache = new HashMap<>();

    @Override
    public void onInsert(Model model, ModelQueryBuilder<? extends Model> builder) {
        cacheModelFields(model);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<String> dirtyCols = model.getDirtyColumns();
        List<String> cached = new ArrayList<>(
            createdTimestampColCache.getOrDefault(model.getClass(), Collections
                .emptyList()));
        if (!cached.isEmpty()) {
            cached.removeAll(dirtyCols); // Remove dirty cols
            log.debug("Updating created at timestamps on columns {} on {}",
                String.join(",", cached), dirtyCols.getClass());
        }
        cached.forEach(col -> model.setColumn(col, now));
    }

    @Override
    public void onUpdate(Model model, ModelQueryBuilder<? extends Model> builder) {
        cacheModelFields(model);
        Timestamp now = new Timestamp(System.currentTimeMillis());
        List<String> dirtyCols = model.getDirtyColumns();
        List<String> cached = new ArrayList<>(
            updatedTimestampColCache.getOrDefault(model.getClass(), Collections
                .emptyList()));
        if (!cached.isEmpty()) {
            cached.removeAll(dirtyCols); // Remove dirty cols
            log.debug("Updating updated at timestamps on columns {} on {}",
                String.join(",", cached), dirtyCols.getClass());
        }
        cached.forEach(col -> model.setColumn(col, now));
    }


    @Override
    public String name() {
        return Constants.ENHANCER_TIMESTAMPS;
    }

    private void cacheModelFields(Model model) {
        if (cachedModels.contains(model)) {
            return; // Don't re-cache fields
        }
        List<String> created = new ArrayList<>();
        List<String> updated = new ArrayList<>();
        for (Field f : model.getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(CreatedAt.class) || f.isAnnotationPresent(UpdatedAt.class)) {
                if (f.getType() != Timestamp.class) {
                    log.warn("Field {} in {} is not a timestamp", f.getName(), model.getClass());
                    continue;
                }
                if (f.isAnnotationPresent(CreatedAt.class)) {
                    created.add(ModelUtils.getColumnName(f));
                }
                if (f.isAnnotationPresent(UpdatedAt.class)) {
                    updated.add(ModelUtils.getColumnName(f));
                }
            }
        }
        cachedModels.add(model.getClass());
        createdTimestampColCache.put(model.getClass(), created);
        updatedTimestampColCache.put(model.getClass(), updated);
    }

    /**
     * Marker interface designating this field to be updated when the model is created
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface CreatedAt {

    }

    /**
     * Marker interface designating this field to be updated when the model is modified
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    public @interface UpdatedAt {

    }
}
