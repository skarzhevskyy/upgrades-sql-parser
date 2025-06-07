/*
 * Created on Aug 20, 2018
 * @author vlads
 */
package com.example.ddl;

import static org.assertj.core.api.Assertions.assertThat;

import net.sf.jsqlparser.JSQLParserException;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SQLDialectParserTest {

    private static final Logger log = LoggerFactory.getLogger(SQLDialectParserTest.class);

    private DialectAdapter createDialect(DatabaseType databaseType) {
        switch (databaseType) {
            case Oracle:
                return new DialectAdapterOracle();
            case PostgreSQL:
                return new DialectAdapterPostgreSQL();
            case HSQLDB:
                return new DialectAdapterHSQL();
            case H2:
                return new DialectAdapterH2();
            default:
                throw new IllegalArgumentException();
        }
    }

    private void validate(DatabaseType databaseType, String tableName, String sql, String expected) {
        String constraintName = tableName + "_CK";
        String normalized = createDialect(databaseType).normalizeCheckConstraintsSQL(sql, tableName, constraintName);
        try {
            assertThat(normalized).isEqualTo(expected);
        } catch (AssertionError e) {
            log.error("Normalized: {}", normalized);
            log.error("  Expected: {}", expected);
            throw e;
        }

        String normalizedSTDExpr = SQLExpressionUtils.normalizeCondExpressionSQL(normalized);
        String normalizedExpected = SQLExpressionUtils.normalizeCondExpressionSQL(expected);

        try {
            assertThat(normalizedExpected).isEqualTo(normalizedSTDExpr);
        } catch (AssertionError e) {
            log.error("Normalized: {}", normalizedSTDExpr);
            log.error("  Expected: {}", normalizedExpected);
            throw e;
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

    private void validateNegative(DatabaseType databaseType, String tableName, String sql, String expected) {
        String normalized = createDialect(databaseType).normalizeCheckConstraintsSQL(sql, tableName, null);
        try {
            assertThat(normalized).isNotEqualTo(expected);
        } catch (AssertionError e) {
            log.error("Normalized: {}", normalized);
            log.error("  Expected: {}", expected);
            throw e;
        }

        String normalizedSTDExpr = SQLExpressionUtils.normalizeCondExpressionSQL(normalized);
        String normalizedExpected = SQLExpressionUtils.normalizeCondExpressionSQL(expected);

        assertThat(normalizedExpected).isNotEqualTo(normalizedSTDExpr);

    }

    @Test
    void testNormalize() {
        validateNormalized(DatabaseType.PostgreSQL, null,
                "(((ID_DC =   'c1'  )))",
                "ID_DC = 'c1'");

        validateNormalized(DatabaseType.HSQLDB, null,
                "(ID_DC = 'c1' AND (ID_DC = 'c1'))",
                "ID_DC = 'c1' AND ID_DC = 'c1'");

        validateNormalized(DatabaseType.HSQLDB, null,
                "(a =b OR (c = d))",
                "a = b OR c = d");

        validateNormalized(DatabaseType.HSQLDB, null,
                "(a = b AND (c = d) or e = f ))",
                "e = f OR a = b AND c = d");

        validateNormalized(DatabaseType.HSQLDB, null,
                "((a IN ('c1', 'c2')))",
                "a IN ('c1', 'c2')");

        validateNormalized(DatabaseType.HSQLDB, null,
                "NOT (a IN ('c1', 'c2'))",
                "a NOT IN ('c1', 'c2')");

        validateNormalized(DatabaseType.HSQLDB, null,
                "b or (NOT (a IN ('c1', 'c2')))",
                "b OR a NOT IN ('c1', 'c2')");
    }

    @Test
    void testNormalizeCheckConstraintsPostgreSQL() {
        validate(DatabaseType.PostgreSQL, null,
                "((tp) = ANY ((ARRAY['employee'])))",
                "(tp IN ('employee'))");

        validate(DatabaseType.PostgreSQL, null,
                "((tp)::text = ANY ((ARRAY['employee'::character varying, 'manager'::character varying])::text[]))",
                "(tp IN ('employee', 'manager'))");

        // Old PostgreSQL 9.3 ...
        validate(DatabaseType.PostgreSQL, null,
                "((tp)::text = ANY (ARRAY[('employee'::character varying)::text, ('manager'::character varying)::text]))",
                "(tp IN ('employee', 'manager'))");

    }

    @Test
    void testNormalizeCheckConstraintsPolymorphicNotNullPostgreSQL10() {
        validate(DatabaseType.PostgreSQL, null,
                "((((id_dscr)::text = ANY ((ARRAY['AA'::character varying, 'BB'::character varying])::text[])) AND (name IS NOT NULL)) OR ((id_dscr)::text <> ALL ((ARRAY['AA'::character varying, 'BB'::character varying])::text[])))",
                "(((id_dscr IN ('AA', 'BB')) AND (name IS NOT NULL)) OR (id_dscr NOT IN ('AA', 'BB')))");

    }

    @Test
    void testNormalizeCheckConstraintsPolymorphicNotNullPostgreSQL11() {
        // PostgreSQL 10
        validate(DatabaseType.PostgreSQL, null,
                "(((((id_dscr)::text = ANY ((ARRAY['A'::character varying, 'B'::character varying])::text[])) AND (name IS NOT NULL)) OR ((id_dscr)::text <> ALL ((ARRAY['A'::character varying, 'B'::character varying])::text[]))))",
                "((((id_dscr IN ('A', 'B')) AND (name IS NOT NULL)) OR (id_dscr NOT IN ('A', 'B'))))");

        // PostgreSQL 11
        validate(DatabaseType.PostgreSQL, null,
                "(((((id_dscr)::text = ANY (ARRAY[('A'::character varying)::text, ('B'::character varying)::text])) AND (name IS NOT NULL)) OR ((id_dscr)::text <> ALL (ARRAY[('A'::character varying)::text, ('B'::character varying)::text]))))",
                "((((id_dscr IN ('A', 'B')) AND (name IS NOT NULL)) OR (id_dscr NOT IN ('A', 'B'))))");
    }

    @Test
    void testNormalizeCheckConstraintsHSQL() {
        validate(DatabaseType.HSQLDB, "TESTDDL_ADD",
                "(PUBLIC.TESTDDL_ADD.STATUS) IN (('ACTIVE'),('DEACTIVATED'),('SUSPENDED'))",
                "STATUS IN ('ACTIVE', 'DEACTIVATED', 'SUSPENDED')");
    }

    @Test
    void testNormalizeCheckConstraintsH2() {
        //H2 1.4.196
        validate(DatabaseType.H2, "TESTDDL",
                "ALTER TABLE PUBLIC.TESTDDL ADD CONSTRAINT PUBLIC.TESTDDL_CK CHECK(V = 'c1')",
                "(V = 'c1')");
        // H2 1.4.199
        validate(DatabaseType.H2, "TESTDDL",
                "ALTER TABLE \"PUBLIC\".\"TESTDDL\" ADD CONSTRAINT \"PUBLIC\".\"TESTDDL_CK\" CHECK(\"V\" = 'c1')",
                "(V = 'c1')");
    }

    @Test
    void testNormalizeCheckConstraintsWithOrHSQL() throws JSQLParserException {
        validate(DatabaseType.HSQLDB, "n/a",
                "(((ID_DC = 'c1') AND (NAME IS NOT NULL)) OR (ID_DC != 'c1'))",
                "(((ID_DC = 'c1') AND (NAME IS NOT NULL)) OR (ID_DC != 'c1'))");

        validateNormalized(DatabaseType.HSQLDB, "n/a",
                "(((ID_DC = 'c1') AND (NAME IS NOT NULL)) OR (ID_DC != 'c1'))",
                "ID_DC != 'c1' OR ID_DC = 'c1' AND NAME IS NOT NULL");

        // Add spaces
        validateNormalized(DatabaseType.HSQLDB, "n/a",
                "(((ID_DC  =  'c1') AND   (NAME IS NOT NULL)) OR (ID_DC != 'c1'))",
                "ID_DC != 'c1' OR ID_DC = 'c1' AND NAME IS NOT NULL");

        validateNegative(DatabaseType.HSQLDB, "n/a",
                "((ID_DC = 'c1') OR ((ID_DC = 'c1') AND (NAME IS NOT NULL)))",
                "((ID_DC != 'c1') OR ((ID_DC = 'c1') AND (NAME IS NOT NULL)))");
    }

    @Test
    void testNormalizeNotInHSQL() throws JSQLParserException {
        validateNormalized(DatabaseType.HSQLDB, "n/a",
                "NOT (ID_DC IN ('c1', 'c2'))",
                "ID_DC NOT IN ('c1', 'c2')");

        validateNormalized(DatabaseType.HSQLDB, "n/a",
                "(ID_DC NOT IN ('c1', 'c2'))",
                "ID_DC NOT IN ('c1', 'c2')");

        validateNormalized(DatabaseType.HSQLDB, "n/a",
                "((ID_DC = 'c1') AND (NAME IS NOT NULL))",
                "ID_DC = 'c1' AND NAME IS NOT NULL");

        validateNormalized(DatabaseType.HSQLDB, "n/a",
                "((ID_DC = 'c1') or (NAME IS NOT NULL))",
                "ID_DC = 'c1' OR NAME IS NOT NULL");
    }

}
