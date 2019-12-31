package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.model.annotations.Enhancer;
import com.mrkirby153.bfs.model.enhancers.SoftDeleteEnhancer;
import jdk.internal.joptsimple.internal.Strings;
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

    public static <T extends SoftDeletingModel> ModelQueryBuilder<T> withTrashed(Class<T> clazz) {
        ModelQueryBuilder<T> mqb = new ModelQueryBuilder<>(clazz);
        mqb.withoutEnhancer(Constants.ENHANCER_SOFT_DELETE);
        return mqb;
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
            Strings.join(deletedAtCols, ", "));
        if (deletedAtCols.size() > 1) {
            log.warn(
                "Found more than 1 soft deleting columns: {}. This may cause unintended side effects",
                Strings.join(deletedAtCols, ", "));
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
     * Marker annotation designating fields that should be considered DeletedAt fields
     */
    @Target(ElementType.FIELD)
    @Retention(RetentionPolicy.RUNTIME)
    public @interface SoftDeleteField {

    }
}
