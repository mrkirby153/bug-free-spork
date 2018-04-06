package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.annotations.PrimaryKey;
import com.mrkirby153.bfs.annotations.Table;
import com.mrkirby153.bfs.sql.QueryBuilder;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
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

    @Column("created_at")
    public Timestamp createdAt;
    @Column("updated_at")
    public Timestamp updatedAt;
    /**
     * If the model should automatically set <code>created_at</code> and <code>updated_at</code> fields
     */
    protected boolean timestamps = true;

    /**
     * Gets a list of all the models matching the query
     *
     * @param modelClass The model class
     * @param column     The column to query
     * @param operator   The comparison to perform
     * @param data       The data to check
     *
     * @return A list of models matching the query or an empty array if none exist
     */
    public static <T extends Model> List<T> get(Class<T> modelClass, String column, String operator, Object data) {
        return get(modelClass, new ModelOption(column, operator, data));
    }

    /**
     * Gets a list of all the models matching the query
     *
     * @param modelClass The model class
     * @param column     The column to query
     * @param data       The data to check
     *
     * @return A list of models matching the query or an empty array if none exist
     */
    public static <T extends Model> List<T> get(Class<T> modelClass, String column, Object data) {
        return get(modelClass, new ModelOption(column, "=", data));
    }

    /**
     * Gets all the elements matching the query
     *
     * @param modelClass The model class
     * @param pairs      The comparisons to do
     *
     * @return A list of models or an empty array if none exist
     */
    public static <T extends Model> List<T> get(Class<T> modelClass, ModelOption... pairs) {
        try {
            ArrayList<T> list = new ArrayList<>();
            T instance = modelClass.newInstance();
            QueryBuilder builder = new QueryBuilder();
            builder.table(instance.getTable());
            builder.select(instance.getColumnData().keySet().toArray(new String[0]));
            for (ModelOption option : pairs) {
                builder.where(option.getColumn(), option.getOperator(), option.getData());
            }
            ResultSet rs = builder.get();
            while (rs.next()) {
                HashMap<String, Object> data = new HashMap<>();
                for (String column : instance.getColumnData().keySet()) {
                    data.put(column, rs.getObject(column));
                }
                T newInstance = modelClass.newInstance();
                newInstance.setData(data);
                list.add(newInstance);
            }
            return list;
        } catch (InstantiationException | IllegalAccessException | SQLException e) {
            e.printStackTrace();
        }
        return new ArrayList<>();
    }

    /**
     * Gets the first element matching the query
     *
     * @param modelClass The model class
     * @param column     The column to query
     * @param operator   The comparison to perform
     * @param data       The data to check
     *
     * @return The first element matching the query or null
     */
    public static <T extends Model> T first(Class<T> modelClass, String column, String operator,
        Object data) {
        List<T> list = get(modelClass, column, operator, data);
        if (list.size() < 1) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Gets the first element matching the query
     *
     * @param modelClass The model class
     * @param column     The column to query
     * @param data       The data to check
     *
     * @return The first element matching the query or null
     */
    public static <T extends Model> T first(Class<T> modelClass, String column, Object data){
        return first(modelClass, column, "=", data);
    }

    /**
     * Gets the first element matching the query
     *
     * @param modelClass The model class
     * @param pairs      The comparisons to do
     *
     * @return The first element matching the query or null
     */
    public static <T extends Model> T first(Class<T> modelClass, ModelOption... pairs) {
        List<T> list = get(modelClass, pairs);
        if (list.size() < 1) {
            return null;
        }
        return list.get(0);
    }

    /**
     * Parses a {@link ResultSet} into a model
     *
     * @param clazz The class
     * @param rs    The result set
     *
     * @return The model or null
     */
    public static <T extends Model> T parse(Class<T> clazz, ResultSet rs) {
        try {
            T instance = clazz.newInstance();
            if (rs.next()) {
                HashMap<String, Object> data = new HashMap<>();
                for (String table : instance.getColumnData().keySet()) {
                    data.put(table, rs.getObject(table));
                }
                instance.setData(data);
            }
            return instance;
        } catch (SQLException | InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return null;
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
            throw new IllegalArgumentException(String.format("The model %s does not have an @Table annotation!", this.getClass()));
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
