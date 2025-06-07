package com.example.ddl;

import org.apache.commons.lang3.StringUtils;

public class DialectAdapterH2 implements DialectAdapter {

    @Override
    public String normalizeCheckConstraintsSQL(String checkConstraintSQL, String tableName, String constraintName) {
        // Input: ALTER TABLE PUBLIC.TESTDDL_ADD ADD CONSTRAINT PUBLIC.TESTDDL_ADD_STATUS_E_CK CHECK(STATUS IN('ACTIVE', 'DEACTIVATED', 'SUSPENDED')) NOCHECK
        String normalized = checkConstraintSQL;

        String fragment = constraintName + " CHECK";
        if (normalized.contains(fragment)) {
            normalized = normalized.substring(normalized.indexOf(fragment) + fragment.length());
        } else {
            fragment = constraintName + "\" CHECK";
            if (normalized.contains(fragment)) {
                normalized = normalized.substring(normalized.indexOf(fragment) + fragment.length());
            }
        }
        normalized = normalized
                .replaceAll("PUBLIC\\." + "(\\w+)\\s+", "$1 ") // remove name prefix  in columns e.g. PUBLIC.STATUS_E_CK.
                .replaceAll("\\((\\w+)\\)", "$1") // cleanup name in extra brackets: (col_name) -> col_name
                .replaceAll("\"(\\w+)\"", "$1") // cleanup quoted identifiers: "col_name" -> col_name
                .replace(" <> ", " != ")
                .replaceAll("\\s+", " ")
                .replace("IN(", "IN (");

        normalized = StringUtils.removeEndIgnoreCase(normalized, " NOCHECK");

        return normalized;
    }

}
