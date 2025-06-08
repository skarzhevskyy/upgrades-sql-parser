package com.example.ddl;

import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.StringProvider;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.parser.ParserKeywordsUtils;
import net.sf.jsqlparser.expression.Expression;
import org.junit.jupiter.api.Test;

public class TestParserConfigTest {
    
    @Test
    void testReservedKeywordValue() throws Exception {
        String sqlCondExpr = "(value IN ('c1', 'c2'))";
        
        System.out.println("Testing reserved keyword 'value' in: " + sqlCondExpr);
        
        // First try with CCJSqlParserUtil (the current failing approach)
        try {
            Expression parseExpression = CCJSqlParserUtil.parseCondExpression(sqlCondExpr);
            System.out.println("CCJSqlParserUtil SUCCESS: " + parseExpression);
        } catch (Exception e) {
            System.out.println("CCJSqlParserUtil FAILED: " + e.getMessage());
        }
        
        // Now try with CCJSqlParser directly
        try {
            CCJSqlParser parser = new CCJSqlParser(new StringProvider(sqlCondExpr));
            Expression parseExpression = parser.SimpleExpression();
            System.out.println("CCJSqlParser direct SUCCESS: " + parseExpression);
        } catch (Exception e) {
            System.out.println("CCJSqlParser direct FAILED: " + e.getMessage());
        }
        
        // Try with CCJSqlParser with all complex parsing allowed
        try {
            CCJSqlParser parser = new CCJSqlParser(new StringProvider(sqlCondExpr))
                .withAllowComplexParsing(true);
            Expression parseExpression = parser.SimpleExpression();
            System.out.println("CCJSqlParser with allowComplexParsing SUCCESS: " + parseExpression);
        } catch (Exception e) {
            System.out.println("CCJSqlParser with allowComplexParsing FAILED: " + e.getMessage());
        }
        
        // Check what reserved keywords exist
        System.out.println("\nReserved keywords check:");
        try {
            // Try different approaches to see what's available
            System.out.println("Available methods in ParserKeywordsUtils:");
            java.lang.reflect.Method[] methods = ParserKeywordsUtils.class.getMethods();
            for (java.lang.reflect.Method method : methods) {
                if (method.getName().contains("eserved")) {
                    System.out.println("  " + method.getName() + " params: " + java.util.Arrays.toString(method.getParameterTypes()));
                }
            }
        } catch (Exception e) {
            System.out.println("Could not explore methods: " + e.getMessage());
        }
    }
}