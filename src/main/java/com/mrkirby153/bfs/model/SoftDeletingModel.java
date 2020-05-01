package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.model.annotations.Enhancer;
import com.mrkirby153.bfs.model.enhancers.SoftDeleteEnhancer;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Enhancer(SoftDeleteEnhancer.class)
public class SoftDeletingModel extends Model {

    private static final Map<Class<? extends Model>, List<String>> softDeletingColumnCache = new ConcurrentHashMap<>();

    private transient List<String> deletedAtCols = new ArrayList<>();

    @Getter
    @Setter
    private boolean forced = false;

    public SoftDeletingModel() {
        deletedAtCols.addAll(getDeletedAtCols(getClass()));
    }

    /**
     * Gets a list of deleted at columns
     *
     * @param clazz The class
     *
     * @return The columns that should be deleted at
     */
    public static List<String> getDeletedAtCols(Class<? extends Model> clazz) {
        return softDeletingColumnCache.computeIfAbsent(clazz, modelClazz -> {
            List<String> columns = Arrays.stream(modelClazz.getDeclaredFields())
                .peek(f -> f.setAccessible(true))
                .filter(f -> f.isAnnotationPresent(SoftDeleteField.class))
                .map(ModelUtils::getColumnName).collect(Collectors.toList());
            log.trace("{} has {} soft deleting fields: {}", modelClazz, columns.size(),
                String.join(", ", columns));
            if (columns.size() > 1) {
                log.warn(
                    "{} has more than 1 soft delete column. This may cause unintentional side effects",
                    modelClazz);
            }
            return columns;
        });
    }

    /**
     * Constructs a query builder that includes trashed data
     *
     * @param clazz The type of model
     *
     * @return The query builder
     */
    public static <T extends SoftDeletingModel> SoftDeletingModelQueryBuilder<T> withTrashed(
        Class<T> clazz) {
        return new SoftDeletingModelQueryBuilder<>(clazz);
    }

    public void touchDeletedAt() {
        Timestamp now = new Timestamp(System.currentTimeMillis());
        deletedAtCols.stream().filter(col -> !this.isColumnDirty(col)).forEach(col -> {
            log.debug("Touching soft delete field {}", col);
            setColumn(col, now);
        });
    }

    /**
     * Restores a model that was previously soft deleted
     */
    public void restore() {
        setExists(true);
        getDeletedAtCols(getClass()).forEach(col -> {
            setColumn(col, null);
        });
        save();
    }

    /**
     * Force deletes a model
     */
    public void forceDelete() {
        setForced(true);
        delete();
        setForced(false);
    }

    /**
     * Checks if a model is trashed. A model is considered trashed if any field annotated with
     * {@link SoftDeleteField} is not null
     *
     * @return True if the model is trashed
     */
    public boolean isTrashed() {
        boolean trashed = false;
        for (String col : getDeletedAtCols(getClass())) {
            if (getData(col) != null) {
                trashed = true;
            }
        }
        return trashed;
    }

    /**
     * Marker annotation designating fields that should be considered DeletedAt fields
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SoftDeleteField {

    }
}
