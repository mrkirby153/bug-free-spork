package com.mrkirby153.bfs.model;

public class ModelOption {

    private String column;
    private String operator;
    private Object data;

    public ModelOption(String column, String operator, Object data) {
        this.column = column;
        this.operator = operator;
        this.data = data;
    }

    public String getColumn() {
        return column;
    }

    public String getOperator() {
        return operator;
    }

    public Object getData() {
        return data;
    }
}
