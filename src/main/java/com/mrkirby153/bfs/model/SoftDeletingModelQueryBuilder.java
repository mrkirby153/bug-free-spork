package com.mrkirby153.bfs.model;

import com.mrkirby153.bfs.Pair;
import com.mrkirby153.bfs.query.QueryBuilder;
import com.mrkirby153.bfs.query.elements.JoinElement.Type;
import com.mrkirby153.bfs.query.elements.OrderElement.Direction;
import com.mrkirby153.bfs.query.grammar.Grammar;

import java.util.ArrayList;
import java.util.List;

/**
 * Query builder for operating on soft deleted models
 * @param <T>
 */
public class SoftDeletingModelQueryBuilder<T extends SoftDeletingModel> extends
    ModelQueryBuilder<T> {

    public SoftDeletingModelQueryBuilder(Class<T> clazz) {
        super(clazz);
        withoutEnhancer(Constants.ENHANCER_SOFT_DELETE);
    }

    public SoftDeletingModelQueryBuilder(Grammar grammar, Class<T> clazz) {
        super(grammar, clazz);
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> select(String... columns) {
        super.select(columns);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> from(String table) {
        super.from(table);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> table(String table) {
        super.table(table);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> where(String column, String operator, Object value) {
        super.where(column, operator, value);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> where(String column, Object value) {
        super.where(column, value);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> orWhere(String column, String operator, Object value) {
        super.orWhere(column, operator, value);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> orWhere(String column, Object value) {
        super.orWhere(column, value);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNull(String column) {
        super.whereNull(column);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNull(String column, String bool) {
        super.whereNull(column, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNotNull(String column) {
        super.whereNotNull(column);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNotNull(String column, String bool) {
        super.whereNotNull(column, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereIn(String column, Object[] values, boolean not,
        String bool) {
        super.whereIn(column, values, not, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereIn(String column, Object[] values) {
        super.whereIn(column, values);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereIn(String column, Object[] values, String bool) {
        super.whereIn(column, values, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNotIn(String column, Object[] values) {
        super.whereNotIn(column, values);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNotIn(String column, Object[] values,
        String bool) {
        super.whereNotIn(column, values, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereSub(String column, QueryBuilder builder,
        boolean not,
        String bool) {
        super.whereSub(column, builder, not, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereSub(String column, QueryBuilder builder) {
        super.whereSub(column, builder);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereSub(String column, QueryBuilder builder,
        String bool) {
        super.whereSub(column, builder, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNotSub(String column, QueryBuilder builder) {
        super.whereNotSub(column, builder);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> whereNotSub(String column, QueryBuilder builder,
        String bool) {
        super.whereNotSub(column, builder, bool);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> orderBy(String column, Direction direction) {
        super.orderBy(column, direction);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> limit(long amount) {
        super.limit(amount);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> join(Type type, String table, String first,
        String operator,
        String second) {
        super.join(type, table, first, operator, second);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> leftJoin(String table, String first, String operator,
        String second) {
        super.leftJoin(table, first, operator, second);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> rightJoin(String table, String first, String operator,
        String second) {
        super.rightJoin(table, first, operator, second);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> innerJoin(String table, String first, String operator,
        String second) {
        super.innerJoin(table, first, operator, second);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> outerJoin(String table, String first, String operator,
        String second) {
        super.outerJoin(table, first, operator, second);
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> distinct() {
        super.distinct();
        return this;
    }

    @Override
    public SoftDeletingModelQueryBuilder<T> offset(long amount) {
        super.offset(amount);
        return this;
    }

    /**
     * Bulk restores models
     */
    public void restore() {
        List<Pair<String, Object>> data = new ArrayList<>();
        SoftDeletingModel.getDeletedAtCols(getModelClass()).forEach(col -> {
            data.add(new Pair<>(col, null));
        });
        update(data.toArray(new Pair[0]));
    }
}
