package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.sql.DbRow;
import com.mrkirby153.bfs.sql.QueryBuilder;
import com.mrkirby153.bfs.sql.elements.JoinElement.Type;
import com.mrkirby153.bfs.sql.grammars.Grammar;

import java.util.ArrayList;
import java.util.List;

public class ModelQueryBuilder<T extends Model> extends QueryBuilder {

    private Class<T> modelClass;

    public ModelQueryBuilder(Grammar grammar, Class<T> clazz) {
        super(grammar);
        this.modelClass = clazz;
        try {
            T instance = clazz.newInstance();
            table(instance.getTable());
        } catch (InstantiationException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    public List<T> get() {
        List<DbRow> rows = this.query();
        List<T> results = new ArrayList<>();
        rows.forEach(row -> {
            try {
                T instance = this.modelClass.newInstance();
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
}
