package com.mrkirby153.bugfreespork.model;

import com.google.common.base.CaseFormat;
import com.mrkirby153.bugfreespork.ConnectionFactory;
import com.mrkirby153.bugfreespork.annotations.Column;
import com.mrkirby153.bugfreespork.annotations.PrimaryKey;

import java.beans.Transient;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Model {

    protected static ConnectionFactory factory;

    /**
     * Sets the connection factory to use for statements
     *
     * @param factory The factory
     */
    public static void setConnectionFactory(ConnectionFactory factory) {
        Model.factory = factory;
    }

    /**
     * Gets the columns to store in the database
     *
     * @return The columns to store
     */
    public Map<String, Object> getColumns() {
        HashMap<String, Object> map = new HashMap<>();
        for (Field f : this.getFields()) {
            try {
                map.put(getColumnName(f), f.get(this));
            } catch (IllegalAccessException e) {
                // Ignore probably shouldn't happen
            }
        }
        return map;
    }

    /**
     * Gets the primary key in the model. Annotate a model with {@link PrimaryKey} to set the column
     *
     * @return The name of the models' primary key.
     */
    public String getPrimaryKey() {
        for (Field f : this.getFields()) {
            if (f.isAnnotationPresent(PrimaryKey.class)) {
                return getColumnName(f);
            }
        }
        return "id"; // Default to 'id'
    }

    /**
     * Gets a field's column name. This method first checks if there is a {@link Column} annotation
     * to override the column's name. If the annotation is not present, it uses the field's name in snake case
     *
     * @param field The field
     *
     * @return The field's column name
     */
    private String getColumnName(Field field) {
        return field.isAnnotationPresent(Column.class) ? field.getAnnotation(Column.class).value()
            : CaseFormat.UPPER_CAMEL.to(CaseFormat.LOWER_UNDERSCORE, field.getName());
    }

    /**
     * Gets all fields that are non-transient to store in the database.
     *
     * @return A list of fields to store in the database
     */
    private List<Field> getFields() {
        List<Field> fields = new ArrayList<>();
        for (Field f : this.getClass().getDeclaredFields()) {
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            if (f.isAnnotationPresent(Transient.class)) {
                continue;
            }
            fields.add(f);
        }
        return fields;
    }
}
