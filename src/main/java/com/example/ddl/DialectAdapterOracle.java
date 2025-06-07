package com.example.ddl;

public class DialectAdapterOracle implements DialectAdapter {

    @Override
    public String normalizeCheckConstraintsSQL(String checkConstraintSQL, String tableName, String constraintName) {
        return checkConstraintSQL
                .replaceAll("\\(\\s+'", "('") // Ignore old style of constraints
                .replaceAll("\\s+", " ");
    }

}
