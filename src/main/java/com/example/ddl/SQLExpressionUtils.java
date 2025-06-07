/*
 * Created on Aug 21, 2018
 * @author vlads
 */
package com.example.ddl;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.ParenthesedExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrException;
import net.sf.jsqlparser.schema.Column;
import org.apache.commons.lang3.StringUtils;

public class SQLExpressionUtils {

    private SQLExpressionUtils() {
    }

    public static String normalizeCondExpressionSQL(String sqlCondExpr) {
        try {
            Expression parseExpression = CCJSqlParserUtil.parseCondExpression(sqlCondExpr);
            
            // TODO: Implement full expression normalization with JSQLParser 5.x
            // For now, use basic normalization with string manipulation
            Expression normalized = normalizeExpression(parseExpression);
            return normalized.toString();
        } catch (TokenMgrException | JSQLParserException e) {
            throw new RuntimeException("conditional expression '" + sqlCondExpr + "' parser error", e);
        }
    }
    
    private static Expression normalizeExpression(Expression expression) {
        if (expression instanceof OrExpression orExpr) {
            // Place shorter expression on left as HSQL/H2 does
            Expression left = normalizeExpression(orExpr.getLeftExpression());
            Expression right = normalizeExpression(orExpr.getRightExpression());
            
            if (left.toString().length() > right.toString().length()) {
                return new OrExpression(right, left);
            } else {
                return new OrExpression(left, right);
            }
        } else if (expression instanceof AndExpression andExpr) {
            return new AndExpression(
                normalizeExpression(andExpr.getLeftExpression()),
                normalizeExpression(andExpr.getRightExpression())
            );
        } else if (expression instanceof NotExpression notExpr) {
            Expression innerExpr = notExpr.getExpression();
            
            // Handle NOT (IN expression) -> NOT IN
            if (innerExpr instanceof InExpression inExpression) {
                // NOT (v IN ...) -> v NOT IN ...
                InExpression result = new InExpression();
                result.setLeftExpression(normalizeExpression(inExpression.getLeftExpression()));
                result.setRightExpression(inExpression.getRightExpression());
                result.setNot(true); // Set NOT flag to true
                return result;
            } else {
                // For other expressions, check if it contains an IN expression by string manipulation
                String innerStr = innerExpr.toString();
                if (innerStr.contains(" IN ")) {
                    // Remove outer parentheses if they exist
                    String cleanStr = innerStr;
                    if (cleanStr.startsWith("(") && cleanStr.endsWith(")")) {
                        cleanStr = cleanStr.substring(1, cleanStr.length() - 1);
                    }
                    String transformed = cleanStr.replaceFirst("\\s+IN\\s+", " NOT IN ");
                    try {
                        Expression parsedTransformed = CCJSqlParserUtil.parseCondExpression(transformed);
                        return normalizeExpression(parsedTransformed);
                    } catch (Exception e) {
                        // Manual transformation failed, fall back to original logic
                    }
                }
                
                return new NotExpression(normalizeExpression(innerExpr));
            }
        } else if (expression instanceof ParenthesedExpressionList parenthesedList) {
            // Handle ParenthesedExpressionList by extracting the inner expression
            // This replaces the deprecated Parenthesis class handling
            @SuppressWarnings("deprecation")
            var expressions = parenthesedList.getExpressions();
            if (expressions.size() == 1) {
                // Single expression wrapped in parentheses - normalize and return without parentheses
                Expression innerExpr = (Expression) expressions.get(0);
                return normalizeExpression(innerExpr);
            } else {
                // Multiple expressions - this shouldn't happen for conditional expressions
                // but if it does, return as-is
                return expression;
            }
        } else if (expression instanceof Column tableColumn) {
            return new Column(tableColumn.getTable(), sqlNameUnEscape(tableColumn.getColumnName()));
        } else {
            return expression;
        }
    }
    
    static String sqlNameUnEscape(String sqlName) {
        return StringUtils.strip(sqlName, "`\"");
    }

}
