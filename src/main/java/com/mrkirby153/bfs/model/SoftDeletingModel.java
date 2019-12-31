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
import java.lang.reflect.Field;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Enhancer(SoftDeleteEnhancer.class)
public class SoftDeletingModel extends Model {

    private transient List<String> deletedAtCols = new ArrayList<>();

    @Getter
    @Setter
    private boolean forced = false;

    public SoftDeletingModel() {
        scanForDeletedAt();
    }

    /**
     * Gets a list of deleted at columns
     *
     * @param clazz The class
     *
     * @return The columns that should be deleted at
     */
    public static List<String> getDeletedAtCols(Class<? extends Model> clazz) {
        return Arrays.stream(clazz.getDeclaredFields()).peek(f -> f.setAccessible(true))
            .filter(f -> f.isAnnotationPresent(SoftDeleteField.class))
            .map(ModelUtils::getColumnName).collect(
                Collectors.toList());
    }

    /**
     * Constructs a query builder that includes trashed data
     *
     * @param clazz The type of model
     *
     * @return The query builder
     */
    public static <T extends SoftDeletingModel> SoftDeletingModelQueryBuilder<T> withTrashed(Class<T> clazz) {
        return new SoftDeletingModelQueryBuilder<>(clazz);
    }

    private void scanForDeletedAt() {
        deletedAtCols.clear();
        for (Field f : getClass().getDeclaredFields()) {
            f.setAccessible(true);
            if (f.isAnnotationPresent(SoftDeleteField.class)) {
                deletedAtCols.add(ModelUtils.getColumnName(f));
            }
        }
        log.trace("Identified {} deleted at columns: ({})", deletedAtCols.size(),
            String.join(", ", deletedAtCols));
        if (deletedAtCols.size() > 1) {
            log.warn(
                "Found more than 1 soft deleting columns: {}. This may cause unintended side effects",
                String.join(", ", deletedAtCols));
        }
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
     * Marker annotation designating fields that should be considered DeletedAt fields
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SoftDeleteField {

    }
}
