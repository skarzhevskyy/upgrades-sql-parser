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
            Expression innerExpr = stripParenthesis(notExpr.getExpression());
            if (innerExpr instanceof InExpression inExpression) {
                // NOT v IN -> v NOT IN
                inExpression.setNot(!inExpression.isNot());
                return normalizeExpression(inExpression);
            } else {
                return new NotExpression(normalizeExpression(notExpr.getExpression()));
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
        
        // Remove unnecessary brackets
        if (innerExpr instanceof Parenthesis || 
            innerExpr instanceof NotExpression ||
            innerExpr instanceof EqualsTo ||
            innerExpr instanceof NotEqualsTo ||
            innerExpr instanceof IsNullExpression ||
            innerExpr instanceof InExpression ||
            innerExpr instanceof AndExpression) {
            return normalizeExpression(innerExpr);
        } else if (innerExpr instanceof OrExpression) {
            // Only skip global brackets for OR at the top level
            return normalizeExpression(innerExpr);
        } else {
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
