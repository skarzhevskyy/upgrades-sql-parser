/*
 * Created on Aug. 23, 2022
 * @author vlads
 *
 */
package com.example.ddl;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.sf.jsqlparser.JSQLParserException;

/**
 * This affects DDL for automatic DB migration for constraints on columns with reserved keyword names.
 *
 * This reserved words case used to work in 4.4 and is failing in 4.5..4.6
 *
 * @see SQLDialectParserTest
 */
@Disabled
class SQLDialectParserReservedTest {

    private static final Logger log = LoggerFactory.getLogger(SQLDialectParserReservedTest.class);

    @Test
    void testNormalizeValuesIn() throws JSQLParserException {
        validateNormalized(DatabaseType.Oracle, "n/a",
                "(v IN ('c1', 'c2'))",
                "v IN ('c1', 'c2')");

        // This is failing with jsqlparser-4.5
        validateNormalized(DatabaseType.Oracle, "n/a",
                "(value IN ('c1', 'c2'))",
                "value IN ('c1', 'c2')");
    }

    private DialectAdapter createDialect(DatabaseType databaseType) {
        switch (databaseType) {
        case Oracle:
            return new DialectAdapterOracle();
        case PostgreSQL:
            return new DialectAdapterPostgreSQL();
        default:
            throw new IllegalArgumentException();
        }
    }

    private void validateNormalized(DatabaseType databaseType, String tableName, String sql, String expected) {
        String normalized = createDialect(databaseType).normalizeCheckConstraintsSQL(sql, tableName, null);
        String normalizedSTDExpr = SQLExpressionUtils.normalizeCondExpressionSQL(normalized);
        try {
            assertThat(expected).isEqualTo(normalizedSTDExpr);
        } catch (AssertionError e) {
            log.error("Normalized: [{}]", normalizedSTDExpr);
            log.error("  Expected: [{}]", expected);
            throw e;
        }
    }
}
