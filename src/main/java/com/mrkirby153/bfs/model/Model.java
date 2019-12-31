package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.model.annotations.Column;
import com.mrkirby153.bfs.model.annotations.InheritFields;
import com.mrkirby153.bfs.model.annotations.PrimaryKey;
import com.mrkirby153.bfs.model.annotations.Table;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
public class Model {

    /**
     * The mapping of column names to fields
     */
    private transient Map<String, Field> columns = new HashMap<>();

    /**
     * The state of the model. Used for determining if the model is dirty
     */
    private transient Map<Field, Object> state = new HashMap<>();

    /**
     * If the model exists
     */
    @Setter
    private transient boolean exists = false; // All newly created models do not exist

    private transient ModelQueryBuilder<Model> queryBuilder = new ModelQueryBuilder<>(
        (Class<Model>) this.getClass());


    public Model() {
        discoverColumns();
    }


    /**
     * Checks if this is a valid model declaration.
     *
     * @return True if the model is valid. False if it isn't
     */
    public boolean isModelValid() {
        return this.getClass().isAnnotationPresent(Table.class);
    }

    /**
     * Discover columns on the class
     */
    private void discoverColumns() {
        log.trace("Beginning discovery of columns in {}", getClass());
        this.columns.clear();
        this.state.clear();

        if (this.getClass().isAnnotationPresent(InheritFields.class)) {
            log.trace("Inheriting fields");
            Class<?> c = this.getClass();
            List<Class<?>> classHierarchy = new ArrayList<>();
            while (c != null) {
                classHierarchy.add(0, c);
                c = c.getSuperclass();
            }
            classHierarchy.forEach(this::discoverColumns);
        } else {
            discoverColumns(this.getClass());
        }
    }

    /**
     * Discover columns on the given class
     *
     * @param clazz The given class
     */
    private void discoverColumns(Class<?> clazz) {
        log.trace("Discovering columns in {}", getClass());
        for (Field f : clazz.getDeclaredFields()) {
            if (!f.isAccessible()) {
                f.setAccessible(true);
            }
            int modifiers = f.getModifiers();
            if (Modifier.isTransient(modifiers) || Modifier.isFinal(modifiers) || Modifier
                .isStatic(modifiers)) {
                log.trace("Skipping field {}. Transient? {}, Final? {}, Static? {}", f.getName(),
                    Modifier.isTransient(modifiers),
                    Modifier.isFinal(modifiers),
                    Modifier.isStatic(modifiers));
                // Ignore any transient final or static modifiers
                continue;
            }
            String columnName =
                f.isAnnotationPresent(Column.class) ? f.getAnnotation(Column.class).value()
                    : f.getName();
            log.trace("Discovered column {} on class {}", columnName, clazz);
            this.columns.put(columnName, f);
        }
    }

    /**
     * Sets the model's state cache
     */
    void updateModelState() {
        log.trace("Saving model's state");
        this.state.clear();
        this.columns.forEach((col, field) -> {
            try {
                Object data = field.get(this);
                log.trace("{} = {}", field.getName(), data);
                this.state.put(field, data);
            } catch (Exception e) {
                log.error("Could not update the model's state", e);
            }
        });
        log.trace("Saved the model's state");
    }

    /**
     * Checks if the column is dirty. A column is considered dirty if the saved value is not equal to the current value
     *
     * @param column The name of the column to check
     *
     * @return True if the column is dirty. False if otherwise
     */
    public boolean isColumnDirty(String column) {
        if (!this.columns.containsKey(column)) {
            return true; // A column with no saved state is always dirty
        }
        try {
            Field colField = this.columns.get(column);
            Object currentValue = colField.get(this);
            Object oldValue = this.state.get(colField);
            if (currentValue == null) {
                if (oldValue != null) {
                    return true; // If the value was not null and is now null
                }
            } else {
                if (oldValue == null) {
                    return true; // If the value is now no longer null
                } else {
                    return !currentValue.equals(oldValue);
                }
            }
        } catch (IllegalAccessException e) {
            log.error("Could not check the dirty state of {} on {}", column, this.getClass(), e);
            return true; // A column where we can't determine if its dirty is always dirty
        }
        return false; // If we fall through then the column is not dirty
    }

    /**
     * Checks if the model is dirty
     *
     * @return True if the model is dirty
     *
     * @see Model#isColumnDirty(String)
     */
    public boolean isDirty() {
        return this.columns.keySet().stream().anyMatch(this::isColumnDirty);
    }

    /**
     * Gets a list of dirty columns
     *
     * @return A list of dirty columsn
     *
     * @see Model#isColumnDirty(String)
     */
    public List<String> getDirtyColumns() {
        return this.columns.keySet().stream().filter(this::isColumnDirty)
            .collect(Collectors.toList());
    }

    private String getColumnName(Field f) {
        for (Map.Entry<String, Field> e : this.columns.entrySet()) {
            if (e.getValue().equals(f)) {
                return e.getKey();
            }
        }
        throw new IllegalArgumentException("Column not found for " + f);
    }

    /**
     * Gets the model's primary key
     *
     * @return The primary key or "id" if no field has the {@link PrimaryKey} annotation
     *
     * @throws IllegalStateException If multiple primary keys were found
     */
    public String getPrimaryKey() {
        String key = null;
        for (Field f : this.columns.values()) {
            if (f.isAnnotationPresent(PrimaryKey.class)) {
                if (key == null) {
                    key = getColumnName(f);
                } else {
                    throw new IllegalStateException(
                        "Multiple primary keys found for " + getClass());
                }
            }
        }
        if (key == null) {
            key = Constants.DEFAULT_PRIMARY_KEY;
        }
        return key;
    }

    /**
     * Gets the model's table
     *
     * @return The table of the model
     *
     * @throws IllegalArgumentException If the {@link Table} annotation is missing
     */
    public String getTable() {
        String table = ModelUtils.getTable(this.getClass());
        if (table == null) {
            throw new IllegalStateException(
                String.format("%s has no table. Missing the @Table annotation?", this.getClass()));
        }
        return table;
    }


    /**
     * Gets the model's data for inserting into the database
     *
     * @return A map of the column data
     */
    public Map<String, Object> getColumnData() {
        Map<String, Object> data = new HashMap<>();
        columns.forEach((column, field) -> {
            try {
                Object d = field.get(this);
                data.put(column, d);
            } catch (IllegalAccessException e) {
                log.error("Could not get data for {}", field, e);
            }
        });
        return data;
    }

    /**
     * Sets a column's data
     *
     * @param column The column
     * @param value  The value
     */
    public void setColumn(String column, Object value) {
        Field f = this.columns.get(column);
        if (f == null) {
            throw new IllegalArgumentException(
                String.format("The column %s does not exist on the model", column));
        }
        try {
            f.set(this, value);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException(
                String.format("Could not set the value of %s", column));
        }
    }

    /**
     * Hydrates the data from the database and marks the model as existing
     *
     * @param data The data to hydrate.
     */
    public void hydrate(Map<String, Object> data) {
        data.forEach(this::setColumn);
        updateModelState();
        this.exists = true;
    }

    /**
     * If the model exists
     *
     * @return True if the model exists
     */
    public boolean exists() {
        return exists;
    }

    public Object getData(String column) {
        if (!this.columns.containsKey(column)) {
            throw new IllegalArgumentException(
                String.format("Column %s does not exist on the model", column));
        }
        try {
            return this.columns.get(column).get(this);
        } catch (IllegalAccessException e) {
            log.error("Error getting data for model {}", this.getClass(), e);
        }
        return null;
    }

    /**
     * Saves the model
     */
    public void save() {
        getQueryBuilder().save();
    }

    /**
     * Updates the model
     */
    public void update() {
        if (!exists) {
            throw new IllegalStateException("Cannot update a model that does not exist");
        }
        getQueryBuilder().update();
    }

    /**
     * Creates the model
     */
    public void create() {
        if (exists) {
            throw new IllegalStateException("Cannot create a model that already exists");
        }
        getQueryBuilder().create();
    }

    /**
     * Deletes the model
     */
    public void delete() {
        if(!exists) {
            throw new IllegalStateException("Cannot delete a model that does not exist");
        }
        getQueryBuilder().delete();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName()
            + "[" + this.getTable() + "]"
            + "{"
            + this.getColumnData().entrySet().stream()
            .map(entry -> String.format("%s=%s", entry.getKey(), entry.getValue())).collect(
                Collectors.joining(", "))
            + "}";
    }

    protected ModelQueryBuilder<Model> getQueryBuilder() {
        ModelQueryBuilder<Model> mqb = new ModelQueryBuilder<>(
            (Class<Model>) this.getClass());
        mqb.setModel(this);
        return mqb;
    }
}
