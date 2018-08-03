package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.model.scopes.Scope;
import com.mrkirby153.bfs.sql.DbRow;
import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.JoinElement.Type;
import com.mrkirby153.bfs.sql.grammars.Grammar;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class ModelQueryBuilder<T extends Model> extends QueryBuilder {

    private Class<T> modelClass;

    private T model;

    private HashMap<String, Scope> scopes = new HashMap<>();

    private List<String> appliedScopes = new ArrayList<>();

    private HashMap<String, Scope> removedScopes = new HashMap<>();

    public ModelQueryBuilder(Grammar grammar, Class<T> clazz) {
        super(grammar);
        this.modelClass = clazz;
    }

    public List<T> get() {
        applyScopes(); // Apply scopes to the query
        List<DbRow> rows = this.query();
        List<T> results = new ArrayList<>();
        rows.forEach(row -> {
            try {
                T instance = this.modelClass.newInstance();
                instance.exists = true;
                instance.setData(row);
                results.add(instance);
            } catch (IllegalAccessException | InstantiationException e) {
                e.printStackTrace();
            }
        });
        return results;
    }

    public T first() {
        List<T> results = get();
        if (results.size() == 0) {
            return null;
        }
        return results.get(0);
    }

    @Override
    public ModelQueryBuilder<T> select(String... columns) {
        super.select(columns);
        return this;
    }

    @Override
    public QueryBuilder from(String table) {
        super.from(table);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> table(String table) {
        super.table(table);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> where(String column, String operator, Object value, String bool) {
        super.where(column, operator, value, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> where(String column, String operator, Object value) {
        super.where(column, operator, value);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> where(String column, Object value) {
        super.where(column, value);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> orWhere(String column, String operator, Object value) {
        super.orWhere(column, operator, value);
        return this;
    }

    @Override
    public ModelQueryBuilder orWhere(String column, Object value) {
        super.orWhere(column, value);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNull(String column, boolean not) {
        super.whereNull(column, not);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNull(String column) {
        super.whereNull(column);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> orderBy(String column, String direction) {
        super.orderBy(column, direction);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> limit(long amount) {
        super.limit(amount);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> offset(long amount) {
        super.offset(amount);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> join(Type type, String table, String first, String operator,
        String second) {
        super.join(type, table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> leftJoin(String table, String first, String operator,
        String second) {
        super.leftJoin(table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> rightJoin(String table, String first, String operator,
        String second) {
        super.rightJoin(table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> outerJoin(String table, String first, String operator,
        String second) {
        super.outerJoin(table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> innerJoin(String table, String first, String operator,
        String second) {
        super.innerJoin(table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> distinct() {
        super.distinct();
        return this;
    }

    /**
     * Gets an immutable list of scopes on the model
     *
     * @return The scope list
     */
    public HashMap<String, Scope> getScopes() {
        return new HashMap<>(scopes);
    }

    /**
     * Gets an immutable list of all scopes that have been applied to the model
     *
     * @return The scopes
     */
    public List<String> getAppliedScopes() {
        return new ArrayList<>(appliedScopes);
    }

    /**
     * Gets an immutable list of all scopes that have been removed from the model
     *
     * @return The scopes
     */
    public HashMap<String, Scope> getRemovedScopes() {
        return new HashMap<>(removedScopes);
    }

    /**
     * Adds a list of scopes to the builder
     *
     * @param scopes The scopes to add
     *
     * @return The query builder
     */
    public ModelQueryBuilder<T> addScopes(HashMap<String, Scope> scopes) {
        scopes.forEach((k, v) -> this.scopes.put(k, v));
        return this;
    }

    /**
     * Adds a scope
     *
     * @param name  The scope name
     * @param scope The scope
     *
     * @return The query builder
     */
    public ModelQueryBuilder<T> addScope(String name, Scope scope) {
        scopes.put(name, scope);
        return this;
    }

    /**
     * Removes a scope from the query
     *
     * @param name The name of the scope
     *
     * @return The query builder
     */
    public ModelQueryBuilder<T> withoutScope(String name) {
        Scope s = this.scopes.remove(name);
        if (s != null) {
            this.removedScopes.put(name, s);
        }
        return this;
    }

    /**
     * Apply all scopes to the
     */
    @SuppressWarnings("unchecked")
    private void applyScopes() {
        this.scopes.entrySet().stream().filter(it -> !appliedScopes.contains(it.getKey()))
            .forEach(entry -> {
                entry.getValue().apply(this, model);
                this.appliedScopes.add(entry.getKey());
            });
    }

    protected void setModel(T model) {
        this.model = model;
    }
}
