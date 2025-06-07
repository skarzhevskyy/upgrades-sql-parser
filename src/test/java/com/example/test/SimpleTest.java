package com.example.test;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;

public class SimpleTest {
    public static void main(String[] args) {
        try {
            Expression expr = CCJSqlParserUtil.parseCondExpression("(a = 1)");
            System.out.println("Original: " + expr.toString());
            
            // Let's see what happens with just toString() instead of using ExpressionDeParser
            System.out.println("Simple toString: " + expr.toString());
            
        } catch (JSQLParserException e) {
            e.printStackTrace();
        }
    }
}