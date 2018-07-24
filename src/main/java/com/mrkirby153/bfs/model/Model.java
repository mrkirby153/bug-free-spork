package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.Tuple;
import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.annotations.PrimaryKey;
import com.mrkirby153.bfs.annotations.Table;
import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.Pair;
import com.mrkirby153.bfs.sql.grammars.Grammar;
import com.mrkirby153.bfs.sql.grammars.MySqlGrammar;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A model in the database
 */
public class Model {

    private static Grammar defaultGrammar = new MySqlGrammar();

    // Internal timestamp fields
    @Column("created_at")
    public Timestamp createdAt;
    @Column("updated_at")
    public Timestamp updatedAt;
    /**
     * If the model should automatically set <code>createdAt</code> and <code>updatedAt</code> fields
     */
    protected transient boolean timestamps = true;
    protected transient boolean incrementing = true;
    protected transient boolean exists = false;
    protected transient HashMap<String, Field> columns = new HashMap<>();
    /**
     * The old state of the model
     */
    private transient HashMap<String, Object> oldState = new HashMap<>();

    public Model() {
        discoverColumns();
    }

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
    public static <T extends Model> List<T> get(Class<T> modelClass, String column, String operator,
        Object data) {
        return get(modelClass, new ModelOption(column, operator, data));
    }

    /**
     * Sets the {@link Grammar} to use when querying
     *
     * @param grammar The grammar to set
     */
    public static void setDefaultGrammar(Grammar grammar) {
        defaultGrammar = grammar;
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
        ModelQueryBuilder<T> builder = new ModelQueryBuilder<>(defaultGrammar, modelClass);
        for (ModelOption option : pairs) {
            builder.where(option.getColumn(), option.getOperator(), option.getData());
        }
        return builder.get();
    }

    /**
     * Gets all the models in the database
     *
     * @param modelClass The model class
     *
     * @return A list of all models
     */
    public static <T extends Model> List<T> get(Class<T> modelClass) {
        return get(modelClass, new ModelOption[0]);
    }

    /**
     * Gets all the elements matching the query
     *
     * @param modelClass The model class
     * @param tuples     The tuples (column/value pairs)
     *
     * @return A list of models or an empty array if none exist
     */
    public static <T extends Model> List<T> get(Class<T> modelClass, Tuple<String, ?>... tuples) {
        List<ModelOption> options = new ArrayList<>();
        for (Tuple<String, ?> t : tuples) {
            options.add(new ModelOption(t.first, "=", t.second));
        }
        return get(modelClass, options.toArray(new ModelOption[0]));
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
    public static <T extends Model> T first(Class<T> modelClass, String column, Object data) {
        return first(modelClass, column, "=", data);
    }

    /**
     * Gets the first model in the database
     *
     * @param modelClass The model class
     *
     * @return The first model
     */
    public static <T extends Model> T first(Class<T> modelClass) {
        return first(modelClass, new ModelOption[0]);
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
     * Gets the first element matching the query
     *
     * @param modelClass The model class
     * @param tuples     A list of tuples (Column/Value pairs)
     *
     * @return The first element matching the query, or null
     */
    public static <T extends Model> T first(Class<T> modelClass, Tuple<String, ?>... tuples) {
        List<ModelOption> options = new ArrayList<>();
        for (Tuple<String, ?> tuple : tuples) {
            options.add(new ModelOption(tuple.first, "=", tuple.second));
        }
        return first(modelClass, options.toArray(new ModelOption[0]));
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
     * Gets a {@link ModelQueryBuilder} to perform advanced queries on the model
     *
     * @param modelClass The type of model to get
     *
     * @return A model query builder
     */
    public static <T extends Model> ModelQueryBuilder<T> query(Class<T> modelClass) {
        return new ModelQueryBuilder<>(defaultGrammar, modelClass);
    }

    /**
     * Gets the column's data
     *
     * @return The model's data ready to be stored in the database
     */
    public HashMap<String, Object> getColumnData() {
        HashMap<String, Object> data = new HashMap<>();

        columns.forEach((columnName, field) -> {
            try {
                Object d = field.get(this);
                data.put(columnName, d);
            } catch (IllegalAccessException e) {
                e.printStackTrace();
            }
        });
        if (!timestamps) {
            data.remove("created_at");
            data.remove("updated_at");
        }
        return data;
    }

    /**
     * Checks if the model is dirty
     *
     * @return True if the model is dirty
     */
    public boolean isDirty() {
        for (Map.Entry<String, Object> e : this.getColumnData().entrySet()) {
            Object old = this.oldState.get(e.getKey());
            if (old == null || !old.equals(e.getValue())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Updates the model's previous state
     */
    private void updateState() {
        this.oldState = this.getColumnData();
    }

    public void save() {
        if (!this.isDirty()) {
            return; // Don't bother saving if we're not dirty
        }

        if (this.exists) {
            this.update();
        } else {
            this.create();
        }
        this.updateState();
    }

    /**
     * Updates the data in the database
     */
    public void update() {
        this.updateTimestamps();
        HashMap<String, Object> data = getColumnData();
        new QueryBuilder(defaultGrammar).table(this.getTable())
            .where(getPrimaryKey(), data.get(getPrimaryKey()))
            .update(getDataAsPairs().toArray(new Pair[0]));
    }

    /**
     * Creates the model in the database
     */
    public void create() {
        this.updateTimestamps();
        Pair[] data = getDataAsPairs().toArray(new Pair[0]);
        if (this.incrementing) {
            long generated = new QueryBuilder(defaultGrammar).table(this.getTable())
                .insertWithGenerated(data);
            HashMap<String, Object> d = new HashMap<>();
            d.put(getPrimaryKey(), generated);
            setData(d);
        } else {
            new QueryBuilder(defaultGrammar).table(this.getTable()).insert(data);
        }
        this.exists = true;
        this.updateState();
    }

    /**
     * Deletes the model from the database
     */
    public void delete() {
        this.exists = false;
        new QueryBuilder(defaultGrammar).table(this.getTable())
            .where(this.getPrimaryKey(), "=", this.getColumnData().get(this.getPrimaryKey()))
            .delete();
    }

    /**
     * Gets the current data as an array of {@link Pair}
     *
     * @return The data
     */
    private ArrayList<Pair> getDataAsPairs() {
        ArrayList<Pair> a = new ArrayList<>();
        getColumnData().forEach((k, v) -> {
            a.add(new Pair(k, v));
        });
        return a;
    }

    /**
     * Takes data retrieved from the database and sets it on the model
     *
     * @param data The data received from the database
     */
    public void setData(HashMap<String, Object> data) {
        data.forEach((column, d) -> {
            Field field = this.columns.get(column);
            if (field == null) {
                return;
            }
            try {
                field.set(this, d);
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
        for (Field f : this.columns.values()) {
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
     * Returns if the model exists
     *
     * @return The model
     */
    public boolean exists() {
        return this.exists;
    }

    /**
     * Gets the model's table in the database
     *
     * @return The table of the model
     */
    public String getTable() {
        if (!this.getClass().isAnnotationPresent(Table.class)) {
            throw new IllegalArgumentException(
                String.format("The model %s does not have an @Table annotation!", this.getClass()));
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

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (!exists && !isDirty("created_at")) {
            this.createdAt = now;
        }
        if (!isDirty("updated_at")) {
            this.updatedAt = now;
        }
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
     * Walk the class tree and add all non-transient non-final fields to the cache
     */
    private void discoverColumns() {
        this.columns.clear();
        this.oldState.clear();
        Class c = this.getClass();
        // Walk the class tree
        while (c != null) {
            for (Field field : c.getDeclaredFields()) {
                if (!field.isAccessible()) {
                    field.setAccessible(true);
                }
                if (Modifier.isTransient(field.getModifiers()) || Modifier
                    .isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String columnName = getColumnName(field);
                this.columns.put(columnName, field);
            }
            c = c.getSuperclass();
        }
    }

    /**
     * Checks if a column is dirty
     *
     * @param column The column to check
     *
     * @return True if the column is dirty
     */
    protected boolean isDirty(String column) {
        if (!this.columns.containsKey(column)) {
            return false;
        }
        try {
            Object currentVal = this.columns.get(column).get(this);
            Object oldVal = this.oldState.get(column);
            if (currentVal == null) {
                if (oldVal != null) {
                    return true;
                }
            } else {
                if (oldVal == null) {
                    return true;
                } else {
                    return !currentVal.equals(oldVal);
                }
            }
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
