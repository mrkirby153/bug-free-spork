package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.ConnectionFactory;
import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.annotations.PrimaryKey;
import com.mrkirby153.bfs.annotations.Table;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;

/**
 * A model in the database
 */
public class Model {

    /**
     * The connection factory used for the database
     */
    private static ConnectionFactory connectionFactory;
    @Column("created_at")
    public Timestamp createdAt;
    @Column("updated_at")
    public Timestamp updatedAt;
    /**
     * If the model should automatically set <code>created_at</code> and <code>updated_at</code> fields
     */
    protected boolean timestamps = true;

    /**
     * Gets the global connection factory
     *
     * @return The connection factory
     */
    public static ConnectionFactory getConnectionFactory() {
        return connectionFactory;
    }

    /**
     * Sets the connection factory used globally
     *
     * @param factory The connection factory
     */
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

    /**
     * Gets the column's data
     *
     * @return The model's data ready to be stored in the database
     */
    public HashMap<String, Object> getColumnData() {
        HashMap<String, Object> data = new HashMap<>();

        getAccessibleFields().forEach(field -> {
            try {
                Object d = field.get(this);
                data.put(getColumnName(field), d);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        return data;
    }

    /**
     * Takes data retrieved from the database and sets it on the model
     *
     * @param data The data received from the database
     */
    public void setData(HashMap<String, Object> data) {
        data.forEach((column, d) -> {
            Optional<Field> fieldOptional = getAccessibleFields().stream()
                .filter(f -> getColumnName(f).equals(column)).findFirst();
            if (!fieldOptional.isPresent()) {
                throw new IllegalArgumentException(
                    String.format("The column %s was not found", column));
            }
            Field f = fieldOptional.get();
            try {
                f.set(this, d);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
    }

    /**
     * Gets the model's primary key
     *
     * @return The primary key or "id" if no field has the {@link PrimaryKey} annotation
     */
    public String getPrimaryKey() {
        String key = null;
        for (Field f : getAccessibleFields()) {
            if (f.isAnnotationPresent(PrimaryKey.class)) {
                if (key == null) {
                    key = getColumnName(f);
                } else {
                    throw new IllegalArgumentException(String
                        .format("The model %s has more than one primary key!",
                            this.getClass().getName()));
                }
            }
        }
        if (key == null) {
            return "id";
        } else {
            return key;
        }
    }


    /**
     * Gets the model's table in the database
     *
     * @return The table of the model
     */
    public String getTable() {
        if (!this.getClass().isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException("The model %s does not have an @Table annotation!");
        }
        return this.getClass().getAnnotation(Table.class).value();
    }

    /**
     * Updates the timestamps of this model
     */
    private void updateTimestamps() {
        if (!timestamps) {
            return;
        }

        if (this.createdAt == null) {
            this.createdAt = new Timestamp(System.currentTimeMillis());
        }
        this.updatedAt = new Timestamp(System.currentTimeMillis());
    }

    /**
     * Gets the name of the field's column
     *
     * @param field The field
     *
     * @return The column's name
     */
    private String getColumnName(Field field) {
        if (field.isAnnotationPresent(Column.class)) {
            return field.getAnnotation(Column.class).value();
        } else {
            return field.getName();
        }
    }

    /**
     * Gets a list of all the fields accessible and used for saving/loading
     *
     * @return The fields
     */
    private List<Field> getAccessibleFields() {
        ArrayList<Field> fields = new ArrayList<>();

        ArrayList<Field> allFields = new ArrayList<>();
        allFields.addAll(Arrays.asList(this.getClass().getFields()));
        allFields.addAll(Arrays.asList(this.getClass().getDeclaredFields()));

        allFields.forEach(field -> {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            if (Modifier.isTransient(field.getModifiers()) || Modifier
                .isFinal(field.getModifiers())) {
                return;
            }
            fields.add(field);
        });
        return fields;
    }
}
