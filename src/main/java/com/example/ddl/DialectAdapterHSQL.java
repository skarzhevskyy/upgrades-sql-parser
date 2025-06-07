package com.example.ddl;

public class DialectAdapterHSQL implements DialectAdapter {

    @Override
    public String normalizeCheckConstraintsSQL(String checkConstraintSQL, String tableName, String constraintName) {
        // Input: (PUBLIC.TESTDDL_ADD.STATUS) IN (('ACTIVE'),('DEACTIVATED'),('SUSPENDED'))
        // Input: PUBLIC.TESTDDL_ENUM2.TP='manager'
        return checkConstraintSQL
                .replaceAll("\\('([\\w\\.]+)'\\)", "'$1'") // cleanup value in extra brackets: ('text') -> 'text'
                .replaceAll("PUBLIC\\." + tableName + "\\.(\\w+)", "$1") // remove table name prefix  in columns e.g. PUBLIC.TESTDDL_ADD.
                .replaceAll("\\((\\w+)\\)", "$1") // cleanup name in extra brackets: (col_name) -> col_name
                .replace("','", "', '") // 'A','B' -> 'A', 'B'
                .replace("!='", " != '") // !='A' -> != 'A'
                .replace("='", " = '") // ='A' -> = 'A'
                ;
    }

}
