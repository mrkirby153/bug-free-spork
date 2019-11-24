package com.mrkirby153.bfs.query;

import java.util.HashMap;

public class DbRow extends HashMap<String, Object> {

    public <T> T get(String column) {
        return (T) super.get(column);
    }

    public <T> T get(String column, T def) {
        T res = (T) super.get(column);
        return res == null ? def : res;
    }

    public Long getLong(String column, Number def) {
        return get(column, def).longValue();
    }

    public Long getLong(String column) {
        return get(column);
    }

    public Integer getInt(String column) {
        return get(column);
    }

    public Integer getInt(String col, Number def) {
        return get(col, def).intValue();
    }

    public Float getFloat(String col) {
        return get(col);
    }

    public Float getFloat(String col, Number def) {
        return get(col, def).floatValue();
    }

    public Double getDouble(String col) {
        return get(col);
    }

    public Double getDouble(String col, Number def) {
        return get(col, def).doubleValue();
    }

    public String getString(String col) {
        return get(col);
    }

    public String getString(String col, String def) {
        return get(col, def);
    }

    public <T> T remove(String column) {
        return (T) super.remove(column);
    }

    public <T> T remove(String col, T def) {
        T res = (T) super.remove(col);
        if (res == null) {
            return def;
        }
        return res;
    }

    public DbRow clone() {
        DbRow row = new DbRow();
        row.putAll(this);
        return row;
    }
}
