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
            ExpressionDeParser deparser = new ExpressionDeParser() {
                @Override
                public void visit(OrExpression expr) {
                    // Place shorter expression on left as HSQL/H2 does
                    if (expr.getLeftExpression().toString().length() > expr.getRightExpression().toString().length()) {
                        super.visit(new OrExpression(expr.getRightExpression(), expr.getLeftExpression()));
                    } else {
                        super.visit(expr);
                    }
                }

                public Expression stripParenthesis(Expression expression) {
                    if (expression instanceof Parenthesis) {
                        return stripParenthesis(((Parenthesis) expression).getExpression());
                    } else {
                        return expression;
                    }
                }

                @Override
                public void visit(Column tableColumn) {
                    super.visit(new Column(tableColumn.getTable(), sqlNameUnEscape(tableColumn.getColumnName())));
                }

                @Override
                public void visit(NotExpression notExpr) {
                    if (stripParenthesis(notExpr.getExpression()) instanceof InExpression inExpression) {
                        // NOT v IN -> v NOT IN
                        inExpression.setNot(!inExpression.isNot());
                        visit(inExpression);
                    } else {
                        super.visit(notExpr);
                    }
                }

                @Override
                public void visit(Parenthesis parenthesis) {
                    // remove unnecessary brackets
                    if (parenthesis.getExpression() instanceof Parenthesis) {
                        visit((Parenthesis) parenthesis.getExpression());
                    } else if (parenthesis.getExpression() instanceof NotExpression) {
                        visit((NotExpression) parenthesis.getExpression());
                    } else if (parenthesis.getExpression() instanceof EqualsTo) {
                        visit((EqualsTo) parenthesis.getExpression());
                    } else if (parenthesis.getExpression() instanceof NotEqualsTo) {
                        visit((NotEqualsTo) parenthesis.getExpression());
                    } else if (parenthesis.getExpression() instanceof IsNullExpression) {
                        visit((IsNullExpression) parenthesis.getExpression());
                    } else if (parenthesis.getExpression() instanceof InExpression) {
                        visit((InExpression) parenthesis.getExpression());
                    } else if (parenthesis.getExpression() instanceof AndExpression) {
                        visit((AndExpression) parenthesis.getExpression());
                    } else if ((parenthesis.getExpression() instanceof OrExpression) && (getBuffer().length() == 0)) {
                        // Only skip global brackets for OR
                        visit((OrExpression) parenthesis.getExpression());
                    } else {
                        super.visit(parenthesis);
                    }
                }
            };

            parseExpression.accept(deparser);
            return deparser.getBuffer().toString();
        } catch (TokenMgrException | JSQLParserException e) {
            throw new RuntimeException("conditional expression '" + sqlCondExpr + "' parser error", e);
        }
    }

    static String sqlNameUnEscape(String sqlName) {
        return StringUtils.strip(sqlName, "`\"");
    }

}
