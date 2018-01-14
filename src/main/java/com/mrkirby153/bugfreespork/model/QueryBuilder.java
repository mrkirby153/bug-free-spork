package com.mrkirby153.bugfreespork.model;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

public class QueryBuilder<T extends Model> {

    private Class<T> model;

    private T instance;

    private String primaryKey = "id";

    private List<WhereClause> selectors = new ArrayList<>();

    public QueryBuilder(Class<T> model) {
        this.model = model;
    }

    public QueryBuilder(Class<T> clazz, T model) {
        this.model = clazz;
        this.instance = model;
    }


    public QueryBuilder<T> where(String column, String test, String value) {
        this.selectors.add(new WhereClause(column, test, value));
        return this;
    }

    public QueryBuilder<T> where(String column, String value) {
        return where(column, "=", value);
    }

    public T get() {
        try {
            if (selectors.isEmpty()) {
                primaryKeySelector();
            }
            String query =
                "SELECT " + buildColumnNames(getInstance().getColumnNames()) + " FROM `"
                    + getInstance().getTableName() + "` WHERE " + buildSelectorQuery();

            PreparedStatement statement = Model.factory.getConnection().prepareStatement(query);

            mapSelectors(1, statement);

            ResultSet rs = statement.executeQuery();
            if (rs.next()) {
                for (String column : getInstance().getColumnNames()) {
                    getInstance().set(column, rs.getObject(column));
                }
            } else {
                return null;
            }
            return instance;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void delete() {
        try {
            if (selectors.isEmpty()) {
                primaryKeySelector();
            }

            String query =
                "DELETE FROM `" + getInstance().getTableName() + "` WHERE " + buildSelectorQuery();
            PreparedStatement ps = Model.factory.getConnection().prepareStatement(query);

            mapSelectors(1, ps);

            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public void update() {
        try {
            if (selectors.isEmpty()) {
                primaryKeySelector();
            }

            Map<String, Object> cols = getInstance().getColumns();
            StringBuilder setBuilder = new StringBuilder();
            List<String> columns = getInstance().getColumnNames();
            for (int i = 0; i < columns.size(); i++) {
                if (columns.get(i).equals(primaryKey)) {
                    continue;
                }
                setBuilder.append("`");
                setBuilder.append(columns.get(i));
                setBuilder.append("`");
                setBuilder.append(" = ?");
                if (i + 1 < columns.size()) {
                    setBuilder.append(", ");
                }
            }

            String query =
                "UPDATE `" + getInstance().getTableName() + "` SET " + setBuilder.toString()
                    + " WHERE "
                    + buildSelectorQuery();
            PreparedStatement statement = Model.factory.getConnection().prepareStatement(query);

            AtomicInteger index = new AtomicInteger(1);
            cols.forEach((key, value) -> {
                if (key.equals(primaryKey)) {
                    return;
                }

                try {
                    statement.setObject(index.getAndIncrement(), value);
                } catch (SQLException e) {
                    // Ignore
                }
            });
            statement.setObject(index.getAndIncrement(), cols.get(primaryKey));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void primaryKeySelector() {
        if (instance == null) {
            throw new IllegalArgumentException("Cannot perform an action on an empty model");
        }
        String primaryKey = getInstance().getPrimaryKey();
        Map<String, Object> cols = getInstance().getColumns();

        selectors.add(new WhereClause(
            primaryKey, "=", cols.get(primaryKey).toString()));
    }

    public void insert() {
        try {
            StringBuilder query =
                new StringBuilder(
                    "INSERT INTO `" + getInstance().getTableName() + "` (" + buildColumnNames(
                        getInstance().getColumnNames()) + ") VALUES (");
            for (int i = 0; i < getInstance().getColumnNames().size(); i++) {
                query.append("?");
                if (i + 1 < getInstance().getColumnNames().size()) {
                    query.append(",");
                }
            }
            query.append(")");

            PreparedStatement preparedStatement = Model.factory.getConnection()
                .prepareStatement(query.toString());
            AtomicInteger index = new AtomicInteger(1);
            getInstance().getColumns().values().forEach(v -> {
                try {
                    preparedStatement.setObject(index.getAndIncrement(), v);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            });
            System.out.println("STATEMENT: " + preparedStatement);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean exists() {
        try {
            String query =
                "SELECT 1 FROM `" + getInstance().getTableName() + "` WHERE "
                    + buildSelectorQuery();
            PreparedStatement statement = Model.factory.getConnection().prepareStatement(query);
            mapSelectors(1, statement);

            return statement.executeQuery().next();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildColumnNames(List<String> names) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < names.size(); i++) {
            builder.append("`");
            builder.append(names.get(i));
            builder.append("`");
            if (i + 1 < names.size()) {
                builder.append(",");
            }
        }
        return builder.toString();
    }

    private T getInstance() {
        if (instance != null) {
            return instance;
        } else {
            try {
                instance = this.model.newInstance();
            } catch (InstantiationException | IllegalAccessException e) {
                e.printStackTrace();
            }
            return instance;
        }
    }

    private String buildSelectorQuery() {
        StringBuilder selectorBuilder = new StringBuilder();
        for (int i = 0; i < this.selectors.size(); i++) {
            selectorBuilder.append("`").append(this.selectors.get(i).getColumn()).append("`");
            selectorBuilder.append(this.selectors.get(i).getTest());
            selectorBuilder.append("?");
            if (i + 1 < this.selectors.size()) {
                selectorBuilder.append(" AND ");
            }
        }
        return selectorBuilder.toString();
    }

    private void mapSelectors(int startIndex, PreparedStatement statement) throws SQLException {
        for (WhereClause c : this.selectors) {
            statement.setString(startIndex++, c.getValue());
        }
    }
}
