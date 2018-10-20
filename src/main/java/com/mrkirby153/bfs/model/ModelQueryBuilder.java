package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.model.scopes.Scope;
import com.mrkirby153.bfs.model.scopes.ScopeUtils;
import com.mrkirby153.bfs.sql.DbRow;
import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.JoinElement.Type;
import com.mrkirby153.bfs.sql.elements.Pair;
import com.mrkirby153.bfs.sql.grammars.Grammar;

import java.util.ArrayList;
import java.util.Arrays;
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
        this.table(ModelUtils.getTable(clazz));
    }

    public List<T> get() {
        applyScopes();
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

    /**
     * Inserts many models into the database
     *
     * @param data The data to insert
     *
     * @return A list of generated keys
     */
    public List<Long> insertMany(T[] data) {
        List<List<Pair>> l = new ArrayList<>();
        boolean incrementing = data[0].incrementing;
        for (T t : data) {
            t.updateTimestamps();
            List<Pair> p = new ArrayList<>(Arrays.asList(t.getDataForInsert()));
            l.add(p);
            t.exists = true;
        }
        if (incrementing) {
            List<Long> generated = this.insertBulkWithGenerated(l);
            for (int i = 0; i < generated.size(); i++) {
                data[i].setData(data[i].getPrimaryKey(), generated.get(i));
            }
            return generated;
        } else {
            return new ArrayList<>(data.length);
        }
    }

    @Override
    public ModelQueryBuilder<T> select(String... columns) {
        super.select(columns);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> from(String table) {
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
    public ModelQueryBuilder<T> whereNull(String column) {
        super.whereNull(column);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNull(String column, String bool) {
        super.whereNull(column, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotNull(String column, String bool) {
        super.whereNotNull(column, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotNull(String column) {
        super.whereNotNull(column);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereIn(String column, Object[] values, String bool) {
        super.whereIn(column, values, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereIn(String column, Object[] values) {
        super.whereIn(column, values);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotIn(String column, Object[] values, String bool) {
        super.whereNotIn(column, values, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotIn(String column, Object[] values) {
        super.whereNotIn(column, values);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereSub(String column, QueryBuilder builder, boolean not,
        String bool) {
        super.whereSub(column, builder, not, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereSub(String column, QueryBuilder builder, String bool) {
        super.whereSub(column, builder, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereSub(String column, QueryBuilder builder) {
        super.whereSub(column, builder);
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
     * Adds a scope to the query builder
     *
     * @param scope The scope
     *
     * @return The query builder
     */
    public ModelQueryBuilder<T> addScope(Class<? extends Scope> scope) {
        this.scopes.put(ScopeUtils.getIdentifier(scope), ScopeUtils.getScope(scope));
        return this;
    }

    /**
     * Removes an applied scope from the query
     *
     * @param identifier The identifier of the scope
     *
     * @return The query builder
     */
    public ModelQueryBuilder<T> withoutScope(String identifier) {
        Scope s = this.scopes.remove(identifier);
        if (s != null) {
            this.removedScopes.put(identifier, s);
        }
        return this;
    }

    /**
     * Apply all scopes on the query builder
     *
     * @return The query builder
     */
    @SuppressWarnings("unchecked")
    public ModelQueryBuilder<T> applyScopes() {
        for (Scope scope : this.scopes.values()) {
            scope.apply(this.model, this);
            this.appliedScopes.add(scope.identifier());
        }
        return this;
    }

    protected void setModel(T model) {
        this.model = model;
        this.table(model.getTable());
    }
}
