/*
 * Created on Aug 21, 2018
 * @author vlads
 */
package com.example.ddl;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.NotExpression;
import net.sf.jsqlparser.expression.Parenthesis;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.expression.operators.conditional.OrExpression;
import net.sf.jsqlparser.expression.operators.relational.EqualsTo;
import net.sf.jsqlparser.expression.operators.relational.InExpression;
import net.sf.jsqlparser.expression.operators.relational.IsNullExpression;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.TokenMgrException;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
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
            
            // Strip parentheses to get to the actual expression
            Expression strippedExpr = stripParenthesis(innerExpr);
            
            if (strippedExpr instanceof InExpression inExpression) {
                // NOT (v IN ...) -> v NOT IN ...
                InExpression result = new InExpression();
                result.setLeftExpression(normalizeExpression(inExpression.getLeftExpression()));
                result.setRightExpression(inExpression.getRightExpression());
                result.setNot(true); // Set NOT flag to true
                return result;
            } else {
                // JSQLParser 5.x might structure IN expressions differently
                // Let me check if this is actually an IN expression by looking at the string representation
                String innerStr = innerExpr.toString();
                if (innerStr.contains(" IN ")) {
                    // This is hacky but let's try to transform it manually
                    // Remove outer parentheses first
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
        } else if (expression instanceof Parenthesis parenthesis) {
            return normalizeParenthesis(parenthesis);
        } else if (expression instanceof Column tableColumn) {
            return new Column(tableColumn.getTable(), sqlNameUnEscape(tableColumn.getColumnName()));
        } else {
            return expression;
        }
    }
    
    private static Expression normalizeParenthesis(Parenthesis parenthesis) {
        Expression innerExpr = parenthesis.getExpression();
        
        // Always remove unnecessary brackets for these expression types
        if (innerExpr instanceof Parenthesis || 
            innerExpr instanceof EqualsTo ||
            innerExpr instanceof NotEqualsTo ||
            innerExpr instanceof IsNullExpression ||
            innerExpr instanceof InExpression ||
            innerExpr instanceof Column ||
            innerExpr instanceof NotExpression ||
            innerExpr instanceof AndExpression ||
            innerExpr instanceof OrExpression) {
            // Remove the parentheses and normalize the inner expression
            return normalizeExpression(innerExpr);
        } else {
            // For other types, keep the parentheses but normalize the content
            Parenthesis result = new Parenthesis();
            result.setExpression(normalizeExpression(innerExpr));
            return result;
        }
    }
    
    private static Expression stripParenthesis(Expression expression) {
        if (expression instanceof Parenthesis parenthesis) {
            return stripParenthesis(parenthesis.getExpression());
        } else {
            return expression;
        }
    }

    static String sqlNameUnEscape(String sqlName) {
        return StringUtils.strip(sqlName, "`\"");
    }

}
