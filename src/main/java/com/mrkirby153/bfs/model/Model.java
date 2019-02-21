package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.annotations.Column;
import com.mrkirby153.bfs.annotations.DefaultField;
import com.mrkirby153.bfs.annotations.PrimaryKey;
import com.mrkirby153.bfs.annotations.Table;
import com.mrkirby153.bfs.model.relationships.OneToManyRelationship;
import com.mrkirby153.bfs.model.relationships.OneToOneRelationship;
import com.mrkirby153.bfs.model.relationships.Relationship;
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

    private static Grammar defaultGrammar = new MySqlGrammar();

    // Internal timestamp fields
    @Column("created_at")
    @DefaultField
    public Timestamp createdAt;

    @Column("updated_at")
    @DefaultField
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

    private transient Map<String, Field> defaultFields = new HashMap<>();

    private transient List<String> removeIfNotDefault = new ArrayList<>();

    public Model() {
        discoverColumns();
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
     * Gets the first mode in the database
     *
     * @param modelClass The model class
     *
     * @return The model, or null if none exists
     */
    public static <T extends Model> T first(Class<T> modelClass) {
        return ModelUtils.getQueryBuilderWithScopes(modelClass).first();
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
     * Gets the data with columns removed
     *
     * @return The data
     */
    protected Pair[] getDataForInsert() {
        if (!timestamps) {
            removeIfNotDefault.add("created_at");
            removeIfNotDefault.add("updated_at");
        }
        return getDataAsPairs().stream().filter(pair -> {
            if (this.removeIfNotDefault.contains(pair.getColumn())) {
                if (columns.get(pair.getColumn())
                    .equals(this.defaultFields.get(pair.getColumn()))) {
                    return isDirty(pair.getColumn());
                }
            }
            return true;
        }).toArray(Pair[]::new);
    }

    /**
     * Creates the model in the database
     */
    public void create() {
        this.updateTimestamps();
        Pair[] data = getDataForInsert();
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
        data.forEach(this::setColumnData);
        updateState();
    }

    public void setData(String key, Object value) {
        setColumnData(key, value);
    }


    private void setColumnData(String column, Object d) {
        Field field = this.columns.get(column);
        if (field == null) {
            return;
        }
        try {
            field.set(this, d);
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
                if(Relationship.class.isAssignableFrom(field.getType())) {
                    continue; // Ignore relationship fields
                }
                if (Modifier.isTransient(field.getModifiers()) || Modifier
                    .isFinal(field.getModifiers()) || Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                String columnName = getColumnName(field);
                this.columns.put(columnName, field);
                if (field.isAnnotationPresent(DefaultField.class)) {
                    this.defaultFields.put(columnName, field);
                }
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
            if (val != null) {
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

    protected <T extends Model> OneToOneRelationship<T> hasOne(Class<T> otherModelClass,
        String foreignKey, String localKey) {
        return new OneToOneRelationship<>(this, otherModelClass, localKey, foreignKey);
    }

    protected <T extends Model> OneToManyRelationship<T> hasMany(Class<T> otherModelClass,
        String foreignKey, String localKey) {
        return new OneToManyRelationship<>(this, otherModelClass, localKey, foreignKey);

    }
}
