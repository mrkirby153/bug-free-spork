package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.Tuple;
import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.annotations.PrimaryKey;
import com.mrkirby153.bfs.annotations.Table;
import com.mrkirby153.bfs.model.scopes.Scope;
import com.mrkirby153.bfs.model.traits.HasTimestamps;
import com.mrkirby153.bfs.sql.elements.Pair;
import com.mrkirby153.bfs.sql.grammars.Grammar;
import com.mrkirby153.bfs.sql.grammars.MySqlGrammar;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * A model in the database
 */
public class Model implements HasTimestamps {

    private static Field defaultCreatedAtField;
    private static Field defaultUpdatedAtField;
    private static Grammar defaultGrammar = new MySqlGrammar();

    static {
        try {
            // Declare the default timestamp fields so we can exclude them reflectively
            defaultCreatedAtField = Model.class.getDeclaredField("createdAt");
            defaultUpdatedAtField = Model.class.getDeclaredField("updatedAt");
        } catch (NoSuchFieldException ignored) {
        }
    }

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
    private transient HashMap<String, Scope> scopes = new HashMap<>();

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
    @Deprecated
    public static <T extends Model> List<T> get(Class<T> modelClass, String column, String operator,
        Object data) {
        return get(modelClass, new ModelOption(column, operator, data));
    }

    /**
     * Gets the default {@link Grammar}
     *
     * @return The grammar
     */
    public static Grammar getDefaultGrammar() {
        return defaultGrammar;
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
    @Deprecated
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
    @Deprecated
    public static <T extends Model> List<T> get(Class<T> modelClass, ModelOption... pairs) {
        ModelQueryBuilder<T> builder = ModelUtils.getQueryBuilderWithScopes(modelClass);
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
    @Deprecated
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
    @Deprecated
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
    @Deprecated
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
        return ModelUtils.getQueryBuilderWithScopes(modelClass).first();
    }

    /**
     * Gets the first element matching the query
     *
     * @param modelClass The model class
     * @param pairs      The comparisons to do
     *
     * @return The first element matching the query or null
     */
    @Deprecated
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
    @Deprecated
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
        return ModelUtils.getQueryBuilderWithScopes(modelClass);
    }

    /**
     * Gets a {@link ModelQueryBuilder} for a model
     *
     * @param modelClass The model class
     * @param column     The column
     * @param operator   The comparison operator
     * @param data       The value of the data
     *
     * @return The builder
     */
    public static <T extends Model> ModelQueryBuilder<T> where(Class<T> modelClass, String column,
        String operator, Object data) {
        ModelQueryBuilder<T> qb = ModelUtils.getQueryBuilderWithScopes(modelClass);
        qb.where(column, operator, data);
        return qb;
    }

    /**
     * Gets a {@link ModelQueryBuilder} for a model with a where clause
     *
     * @param modelClass The model class
     * @param column     The column
     * @param data       The data
     *
     * @return The builder
     */
    public static <T extends Model> ModelQueryBuilder<T> where(Class<T> modelClass, String column,
        Object data) {
        return where(modelClass, column, "=", data);
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
        newQueryWithoutScopes().where(getPrimaryKey(), data.get(getPrimaryKey()))
            .update(getDirtyDataAsPairs().toArray(new Pair[0]));
    }

    /**
     * Creates the model in the database
     */
    public void create() {
        this.updateTimestamps();
        Pair[] data = getDataAsPairs().stream().filter(pair -> {
            // Remove the updated_at and created_at field if they're not dirty and the default
            if (!timestamps) {
                if (pair.getColumn().equalsIgnoreCase(getCreatedAt())) {
                    if (columns.get(getCreatedAt()).equals(defaultCreatedAtField)) {
                        return isDirty(getCreatedAt());
                    }
                }
                if (pair.getColumn().equalsIgnoreCase(getUpdatedAt())) {
                    if (columns.get(getUpdatedAt()).equals(defaultUpdatedAtField)) {
                        return isDirty(getUpdatedAt());
                    }
                }
            }
            return true;
        }).toArray(Pair[]::new);
        ModelQueryBuilder modelQueryBuilder = newQueryWithoutScopes();
        if (this.incrementing) {
            long generated = modelQueryBuilder.insertWithGenerated(data);
            HashMap<String, Object> d = new HashMap<>();
            d.put(getPrimaryKey(), generated);
            setData(d);
        } else {
            modelQueryBuilder.insert(data);
        }
        this.exists = true;
        this.updateState();
    }

    /**
     * Deletes the model from the database
     */
    public void delete() {
        newQueryWithoutScopes()
            .where(this.getPrimaryKey(), "=", this.getColumnData().get(this.getPrimaryKey()))
            .delete();
        this.exists = false;
    }

    /**
     * Gets the current data as an array of {@link Pair}
     *
     * @return The data
     */
    private ArrayList<Pair> getDataAsPairs() {
        ArrayList<Pair> a = new ArrayList<>();
        getColumnData().forEach((k, v) -> a.add(new Pair(k, v)));
        return a;
    }

    private ArrayList<Pair> getDirtyDataAsPairs() {
        ArrayList<Pair> a = new ArrayList<>();
        HashMap<String, Object> data = getColumnData();
        getDirtyColumns().forEach(col -> a.add(new Pair(col, data.get(col))));
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
        updateState();
    }

    public void setData(String key, Object value) {
        Field field = this.columns.get(key);
        if (field == null) {
            return;
        }
        try {
            field.set(this, value);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
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

    private List<String> getDirtyColumns() {
        return this.columns.keySet().stream().filter(this::isDirty).collect(Collectors.toList());
    }

    /**
     * Updates the timestamps of this model
     */
    void updateTimestamps() {
        if (!timestamps) {
            return;
        }

        Timestamp now = new Timestamp(System.currentTimeMillis());
        if (!exists && !isDirty(getCreatedAt())) {
            setData(getCreatedAt(), now);
        }
        if (!isDirty(getUpdatedAt())) {
            setData(getUpdatedAt(), now);
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
     * Creates a new {@link ModelQueryBuilder} with all registered scopes applied
     *
     * @return The query builder
     */
    @SuppressWarnings("unchecked")
    private <T extends Model> ModelQueryBuilder<T> newQueryWithScopes() {
        ModelQueryBuilder q = new ModelQueryBuilder<>(defaultGrammar, (Class<T>) this.getClass());
        q.table(this.getTable());
        q.setModel(this);
        return q;
    }

    /**
     * Creates a new {@link ModelQueryBuilder} without all registered scopes applied
     *
     * @return The query builder
     */
    @SuppressWarnings("unchecked")
    private <T extends Model> ModelQueryBuilder newQueryWithoutScopes() {
        ModelQueryBuilder q = new ModelQueryBuilder(defaultGrammar, this.getClass());
        q.table(this.getTable());
        return q;
    }

    /**
     * Adds a scope to be applied to all queries
     *
     * @param scope The scope to add
     */
    protected void addScope(Scope scope, String name) {
        this.scopes.put(name, scope);
    }

    /**
     * Returns an immutable copy of scopes on the model
     *
     * @return The scopes
     */
    public HashMap<String, Scope> getScopes() {
        return new HashMap<>(this.scopes);
    }

    /**
     * Walk the class tree and add all non-transient non-final fields to the cache
     */
    private void discoverColumns() {
        this.columns.clear();
        this.oldState.clear();
        Class c1 = this.getClass();
        List<Class> classes = new ArrayList<>();
        while (c1 != null) {
            classes.add(c1);
            c1 = c1.getSuperclass();
        }
        // Traverse the class tree from Object to its children
        Collections.reverse(classes);
        // Walk the class tree
        for (Class c : classes) {
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

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(this.getClass().getSimpleName());
        sb.append("[");
        sb.append(this.getTable());
        sb.append("]");
        sb.append("(");
        StringBuilder properties = new StringBuilder();
        AtomicInteger index = new AtomicInteger(0);
        this.getColumnData().forEach((key, val) -> {
            properties.append(key).append("=");
            if(val != null){
                properties.append(val.toString());
            } else {
                properties.append("null");
            }
            if (index.getAndIncrement() + 1 < getColumnData().size()) {
                properties.append(", ");
            }
        });
        sb.append(properties.toString()).append(")");
        return sb.toString();
    }
}
