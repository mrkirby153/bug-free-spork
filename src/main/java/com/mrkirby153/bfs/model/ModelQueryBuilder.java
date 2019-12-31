package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.Pair;
import com.mrkirby153.bfs.model.annotations.AutoIncrementing;
import com.mrkirby153.bfs.model.enhancers.EnhancerUtils;
import com.mrkirby153.bfs.query.DbRow;
import com.mrkirby153.bfs.query.QueryBuilder;
import com.mrkirby153.bfs.query.elements.JoinElement.Type;
import com.mrkirby153.bfs.query.elements.OrderElement.Direction;
import com.mrkirby153.bfs.query.grammar.Grammar;
import jdk.internal.joptsimple.internal.Strings;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Slf4j
public class ModelQueryBuilder<T extends Model> extends QueryBuilder {

    private Class<T> modelClass;

    @Setter
    @Getter
    private T model;

    private List<String> enhancersToSkip = new ArrayList<>();

    public ModelQueryBuilder(Class<T> clazz) {
        this(QueryBuilder.MYSQL_GRAMMAR, clazz);
    }

    public ModelQueryBuilder(Grammar grammar, Class<T> clazz) {
        super(grammar);
        this.modelClass = clazz;
        super.table(ModelUtils.getTable(clazz));
    }

    public List<T> get() {
        List<DbRow> rows = this.query();
        List<T> results = new ArrayList<>();
        rows.forEach(row -> {
            try {
                T instance = this.modelClass.getConstructor().newInstance();
                instance.hydrate(row);
            } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
                log.error("Could not instantiate class {}", modelClass, e);
            }
        });
        return results;
    }

    public T first() {
        this.limit(1); // We only want the first result
        List<DbRow> rows = this.query();
        if (rows.size() == 0) {
            return null;
        }
        try {
            T instance = this.modelClass.getConstructor().newInstance();
            instance.hydrate(rows.get(0));
            return instance;
        } catch (InstantiationException | InvocationTargetException | NoSuchMethodException | IllegalAccessException e) {
            log.error("Could not instantiate class {}", modelClass, e);
        }
        return null;
    }

    private void enhance() {
        List<Enhancer> enhancers = EnhancerUtils
            .withoutEnhancers(modelClass, enhancersToSkip.toArray(new String[0]));
        log.trace("Enhancing query builder for {} with {} enhancers: ({})", modelClass,
            enhancers.size(),
            Strings.join(enhancers.stream().map(Enhancer::name).collect(Collectors.toList()), ","));
        log.trace("{} enhancers have been excluded ({})", enhancersToSkip.size(),
            Strings.join(enhancersToSkip, ","));
        enhancers.forEach(enhancer -> enhancer.enhance(this));
    }

    public void withoutEnhancer(String name) {
        this.enhancersToSkip.add(name);
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
        throw new UnsupportedOperationException("Overriding the table of a model is not supported");
    }

    @Override
    public void addBinding(String section, Object data) {
        super.addBinding(section, data);
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
    public ModelQueryBuilder<T> orWhere(String column, Object value) {
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
    public ModelQueryBuilder<T> whereNotNull(String column) {
        super.whereNotNull(column);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotNull(String column, String bool) {
        super.whereNotNull(column, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereIn(String column, Object[] values, boolean not, String bool) {
        super.whereIn(column, values, not, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereIn(String column, Object[] values) {
        super.whereIn(column, values);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereIn(String column, Object[] values, String bool) {
        super.whereIn(column, values, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotIn(String column, Object[] values) {
        super.whereNotIn(column, values);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotIn(String column, Object[] values, String bool) {
        super.whereNotIn(column, values, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereSub(String column, QueryBuilder builder, boolean not,
        String bool) {
        super.whereSub(column, builder, not, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereSub(String column, QueryBuilder builder) {
        super.whereSub(column, builder);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereSub(String column, QueryBuilder builder, String bool) {
        super.whereSub(column, builder, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotSub(String column, QueryBuilder builder) {
        super.whereNotSub(column, builder);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> whereNotSub(String column, QueryBuilder builder,
        String bool) {
        super.whereNotSub(column, builder, bool);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> orderBy(String column, Direction direction) {
        super.orderBy(column, direction);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> limit(long amount) {
        super.limit(amount);
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
    public ModelQueryBuilder<T> innerJoin(String table, String first, String operator,
        String second) {
        super.innerJoin(table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> outerJoin(String table, String first, String operator,
        String second) {
        super.outerJoin(table, first, operator, second);
        return this;
    }

    @Override
    public ModelQueryBuilder<T> distinct() {
        super.distinct();
        return this;
    }

    @Override
    public ModelQueryBuilder<T> offset(long amount) {
        super.offset(amount);
        return this;
    }

    @Override
    public CompletableFuture<List<DbRow>> queryAsync() {
        enhance();
        return super.queryAsync();
    }

    @Override
    public boolean delete() {
        EnhancerUtils.getEnhancers(modelClass).forEach(enhancer -> enhancer.onDelete(model, this));
        String primaryKey = model.getPrimaryKey();
        Object data = model.getData(primaryKey);
        log.trace("Deleting model with primary key {} = {}", primaryKey, data);
        where(primaryKey, data);
        return super.delete();
    }

    public void create() {
        EnhancerUtils.getEnhancers(modelClass).forEach(enhancer -> enhancer.onInsert(model, this));
        log.trace("Creating model");
        if (model == null) {
            throw new IllegalArgumentException("Cannot delete model that does not exist");
        }
        List<Pair<String, Object>> data = model.getDirtyColumns().stream()
            .map(col -> new Pair<>(col, model.getData(col))).collect(Collectors.toList());
        if (modelClass.isAnnotationPresent(AutoIncrementing.class)) {
            long result = insertWithGenerated(data.toArray(new Pair[0]));
            log.trace("Setting auto generated result {}", result);
            model.setColumn(model.getPrimaryKey(), result);
        } else {
            insert(data.toArray(new Pair[0]));
        }
    }

    public void update() {
        EnhancerUtils.getEnhancers(modelClass).forEach(enhancer -> enhancer.onUpdate(model, this));
        if (model == null) {
            throw new IllegalArgumentException("Cannot update model that does not exist");
        }

        List<Pair<String, Object>> data = model.getDirtyColumns().stream()
            .map(col -> new Pair<>(col, model.getData(col))).collect(Collectors.toList());
        where(model.getPrimaryKey(), model.getData(model.getPrimaryKey()));
        update(data.toArray(new Pair[0]));
    }

    public void save() {
        if (model == null) {
            throw new IllegalArgumentException("Cannot save model that does not exist");
        }
        log.trace("Saving model {}", model.getClass());
        if (!model.isDirty()) {
            log.trace("Skipping save. Model is not dirty");
            return;
        }
        if (!model.exists()) {
            create();
        } else {
            update();
        }
        model.updateModelState();
    }
}
