// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlVisitor extends PsiElementVisitor {

    public void visitAdditiveExpression(@NotNull SqlAdditiveExpression o) {
        visitBinaryExpression(o);
    }

    public void visitAggregateExpression(@NotNull SqlAggregateExpression o) {
        visitExpression(o);
    }

    public void visitAliasDeclaration(@NotNull SqlAliasDeclaration o) {
        visitPsiElement(o);
    }

    public void visitAliasHost(@NotNull SqlAliasHost o) {
        visitPsiElement(o);
    }

    public void visitAllOrAnyExpression(@NotNull SqlAllOrAnyExpression o) {
        visitExpression(o);
    }

    public void visitAsterisk(@NotNull SqlAsterisk o) {
        visitPsiElement(o);
    }

    public void visitBetweenExpression(@NotNull SqlBetweenExpression o) {
        visitExpression(o);
    }

    public void visitBinaryExpression(@NotNull SqlBinaryExpression o) {
        visitExpression(o);
    }

    public void visitBooleanLiteral(@NotNull SqlBooleanLiteral o) {
        visitExpression(o);
    }

    public void visitCaseOperand(@NotNull SqlCaseOperand o) {
        visitPsiElement(o);
    }

    public void visitCoalesceExpression(@NotNull SqlCoalesceExpression o) {
        visitExpression(o);
    }

    public void visitCollectionMemberDeclaration(@NotNull SqlCollectionMemberDeclaration o) {
        visitPsiElement(o);
    }

    public void visitCollectionMemberExpression(@NotNull SqlCollectionMemberExpression o) {
        visitExpression(o);
    }

    public void visitComparisonExpression(@NotNull SqlComparisonExpression o) {
        visitExpression(o);
    }

    public void visitConditionalAndExpression(@NotNull SqlConditionalAndExpression o) {
        visitBinaryExpression(o);
    }

    public void visitConditionalNotExpression(@NotNull SqlConditionalNotExpression o) {
        visitExpression(o);
    }

    public void visitConditionalOrExpression(@NotNull SqlConditionalOrExpression o) {
        visitBinaryExpression(o);
    }

    public void visitDatetimeFunctionExpression(@NotNull SqlDatetimeFunctionExpression o) {
        visitExpression(o);
    }

    public void visitDatetimeLiteral(@NotNull SqlDatetimeLiteral o) {
        visitExpression(o);
    }

    public void visitDeleteClause(@NotNull SqlDeleteClause o) {
        visitPsiElement(o);
    }

    public void visitDeleteStatement(@NotNull SqlDeleteStatement o) {
        visitPsiElement(o);
    }

    public void visitDerivedCollectionMemberDeclaration(@NotNull SqlDerivedCollectionMemberDeclaration o) {
        visitPsiElement(o);
    }

    public void visitEmptyCollectionComparisonExpression(@NotNull SqlEmptyCollectionComparisonExpression o) {
        visitExpression(o);
    }

    public void visitExistsExpression(@NotNull SqlExistsExpression o) {
        visitExpression(o);
    }

    public void visitExpression(@NotNull SqlExpression o) {
        visitPsiElement(o);
    }

    public void visitFromClause(@NotNull SqlFromClause o) {
        visitPsiElement(o);
    }

    public void visitFromClauseReferenceList(@NotNull SqlFromClauseReferenceList o) {
        visitAliasHost(o);
    }

    public void visitFunctionArg(@NotNull SqlFunctionArg o) {
        visitPsiElement(o);
    }

    public void visitFunctionInvocationExpression(@NotNull SqlFunctionInvocationExpression o) {
        visitExpression(o);
    }

    public void visitFunctionsReturningNumericsExpression(@NotNull SqlFunctionsReturningNumericsExpression o) {
        visitExpression(o);
    }

    public void visitGeneralCaseExpression(@NotNull SqlGeneralCaseExpression o) {
        visitExpression(o);
    }

    public void visitGroupbyClause(@NotNull SqlGroupbyClause o) {
        visitPsiElement(o);
    }

    public void visitGroupbyItem(@NotNull SqlGroupbyItem o) {
        visitPsiElement(o);
    }

    public void visitHavingClause(@NotNull SqlHavingClause o) {
        visitPsiElement(o);
    }

    public void visitIdentificationVariableDeclaration(@NotNull SqlIdentificationVariableDeclaration o) {
        visitPsiElement(o);
    }

    public void visitIdentifier(@NotNull SqlIdentifier o) {
        visitPsiElement(o);
    }

    public void visitInExpression(@NotNull SqlInExpression o) {
        visitExpression(o);
    }

    public void visitInItem(@NotNull SqlInItem o) {
        visitPsiElement(o);
    }

    public void visitInputParameterExpression(@NotNull SqlInputParameterExpression o) {
        visitExpression(o);
    }

    public void visitInsertFields(@NotNull SqlInsertFields o) {
        visitPsiElement(o);
    }

    public void visitInsertStatement(@NotNull SqlInsertStatement o) {
        visitPsiElement(o);
    }

    public void visitInsertTuple(@NotNull SqlInsertTuple o) {
        visitPsiElement(o);
    }

    public void visitInsertValue(@NotNull SqlInsertValue o) {
        visitPsiElement(o);
    }

    public void visitJoinCondition(@NotNull SqlJoinCondition o) {
        visitPsiElement(o);
    }

    public void visitJoinExpression(@NotNull SqlJoinExpression o) {
        visitExpression(o);
    }

    public void visitJoinSpec(@NotNull SqlJoinSpec o) {
        visitPsiElement(o);
    }

    public void visitLikeExpression(@NotNull SqlLikeExpression o) {
        visitExpression(o);
    }

    public void visitLimitClause(@NotNull SqlLimitClause o) {
        visitPsiElement(o);
    }

    public void visitMultiplicativeExpression(@NotNull SqlMultiplicativeExpression o) {
        visitBinaryExpression(o);
    }

    public void visitNullComparisonExpression(@NotNull SqlNullComparisonExpression o) {
        visitExpression(o);
    }

    public void visitNullExpression(@NotNull SqlNullExpression o) {
        visitExpression(o);
    }

    public void visitNullifExpression(@NotNull SqlNullifExpression o) {
        visitExpression(o);
    }

    public void visitNumericLiteral(@NotNull SqlNumericLiteral o) {
        visitExpression(o);
    }

    public void visitObjectExpression(@NotNull SqlObjectExpression o) {
        visitExpression(o);
    }

    public void visitOffsetClause(@NotNull SqlOffsetClause o) {
        visitPsiElement(o);
    }

    public void visitOrderbyClause(@NotNull SqlOrderbyClause o) {
        visitPsiElement(o);
    }

    public void visitOrderbyItem(@NotNull SqlOrderbyItem o) {
        visitPsiElement(o);
    }

    public void visitParenExpression(@NotNull SqlParenExpression o) {
        visitExpression(o);
    }

    public void visitPathReferenceExpression(@NotNull SqlPathReferenceExpression o) {
        visitReferenceExpression(o);
    }

    public void visitReferenceExpression(@NotNull SqlReferenceExpression o) {
        visitExpression(o);
    }

    public void visitSelectClause(@NotNull SqlSelectClause o) {
        visitPsiElement(o);
    }

    public void visitSelectItem(@NotNull SqlSelectItem o) {
        visitPsiElement(o);
    }

    public void visitSelectStatement(@NotNull SqlSelectStatement o) {
        visitPsiElement(o);
    }

    public void visitSimpleCaseExpression(@NotNull SqlSimpleCaseExpression o) {
        visitExpression(o);
    }

    public void visitSimpleSelectClause(@NotNull SqlSimpleSelectClause o) {
        visitPsiElement(o);
    }

    public void visitSimpleWhenClause(@NotNull SqlSimpleWhenClause o) {
        visitPsiElement(o);
    }

    public void visitStatement(@NotNull SqlStatement o) {
        visitPsiElement(o);
    }

    public void visitStringFunctionExpression(@NotNull SqlStringFunctionExpression o) {
        visitExpression(o);
    }

    public void visitStringLiteral(@NotNull SqlStringLiteral o) {
        visitExpression(o);
    }

    public void visitSubquery(@NotNull SqlSubquery o) {
        visitPsiElement(o);
    }

    public void visitSubqueryExpression(@NotNull SqlSubqueryExpression o) {
        visitExpression(o);
    }

    public void visitSubqueryFromClause(@NotNull SqlSubqueryFromClause o) {
        visitAliasHost(o);
    }

    public void visitTableExpression(@NotNull SqlTableExpression o) {
        visitExpression(o);
    }

    public void visitTableExpressionJoinDeclaration(@NotNull SqlTableExpressionJoinDeclaration o) {
        visitPsiElement(o);
    }

    public void visitTableNameRef(@NotNull SqlTableNameRef o) {
        visitPsiElement(o);
    }

    public void visitTrimSpecification(@NotNull SqlTrimSpecification o) {
        visitPsiElement(o);
    }

    public void visitTypeExpression(@NotNull SqlTypeExpression o) {
        visitExpression(o);
    }

    public void visitTypeLiteral(@NotNull SqlTypeLiteral o) {
        visitPsiElement(o);
    }

    public void visitUnaryArithmeticExpression(@NotNull SqlUnaryArithmeticExpression o) {
        visitExpression(o);
    }

    public void visitUpdateClause(@NotNull SqlUpdateClause o) {
        visitPsiElement(o);
    }

    public void visitUpdateItem(@NotNull SqlUpdateItem o) {
        visitPsiElement(o);
    }

    public void visitUpdateStatement(@NotNull SqlUpdateStatement o) {
        visitPsiElement(o);
    }

    public void visitWhenClause(@NotNull SqlWhenClause o) {
        visitPsiElement(o);
    }

    public void visitWhereClause(@NotNull SqlWhereClause o) {
        visitPsiElement(o);
    }

    public void visitPsiElement(@NotNull PsiElement o) {
        visitElement(o);
    }

}
