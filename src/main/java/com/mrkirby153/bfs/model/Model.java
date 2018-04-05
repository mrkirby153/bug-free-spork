package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.ConnectionFactory;
import com.mrkirby153.bfs.annotations.Column;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Stream;

/**
 * A model in the database
 */
public class Model {

    /**
     * The connection factory used for the database
     */
    private static ConnectionFactory connectionFactory;


    /**
     * Sets the connection factory used globally
     *
     * @param factory The connection factory
     */
    public static void setConnectionFactory(ConnectionFactory factory) {
        connectionFactory = factory;
    }

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
        Stream.of(this.getClass().getDeclaredFields()).forEach(field -> {
            if (!field.isAccessible()) {
                field.setAccessible(true);
            }

            if (Modifier.isTransient(field.getModifiers())) {
                return;
            }
            fields.add(field);
        });
        return fields;
    }
}
