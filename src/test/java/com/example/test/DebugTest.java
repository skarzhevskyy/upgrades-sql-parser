package com.example.test;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import com.example.ddl.SQLExpressionUtils;

public class DebugTest {
    public static void main(String[] args) {
        try {
            // Test the NOT IN case
            String input = "NOT (ID_DC IN ('c1', 'c2'))";
            System.out.println("Input: " + input);
            
            Expression expr = CCJSqlParserUtil.parseCondExpression(input);
            System.out.println("Parsed expression type: " + expr.getClass().getName());
            
            if (expr instanceof NotExpression) {
                NotExpression notExpr = (NotExpression) expr;
                System.out.println("NotExpression inner type: " + notExpr.getExpression().getClass().getName());
                if (notExpr.getExpression() instanceof InExpression) {
                    System.out.println("Found InExpression inside NotExpression");
                } else {
                    System.out.println("Inner expression is not InExpression");
                }
            }
            
            String result = SQLExpressionUtils.normalizeCondExpressionSQL(input);
            System.out.println("Result: " + result);
            
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }
}