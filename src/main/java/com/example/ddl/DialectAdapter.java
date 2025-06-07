package com.example.ddl;

public interface DialectAdapter {

    /**
     * Normalize Check Constraints SQL Expression to common syntaxis understood by jsqlparser
     *
     * @param checkConstraintSQL
     * @param tableName
     * @param constraintName
     * @return
     */
    default String normalizeCheckConstraintsSQL(String checkConstraintSQL, String tableName, String constraintName) {
        return checkConstraintSQL;
    }

}
