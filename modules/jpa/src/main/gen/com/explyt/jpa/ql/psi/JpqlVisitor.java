// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlVisitor extends PsiElementVisitor {

  public void visitAdditiveExpression(@NotNull JpqlAdditiveExpression o) {
    visitBinaryExpression(o);
  }

  public void visitAggregateExpression(@NotNull JpqlAggregateExpression o) {
    visitExpression(o);
  }

  public void visitAliasDeclaration(@NotNull JpqlAliasDeclaration o) {
    visitNameIdentifierOwner(o);
  }

  public void visitAliasHost(@NotNull JpqlAliasHost o) {
    visitPsiElement(o);
  }

  public void visitAllOrAnyExpression(@NotNull JpqlAllOrAnyExpression o) {
    visitExpression(o);
  }

  public void visitBetweenExpression(@NotNull JpqlBetweenExpression o) {
    visitExpression(o);
  }

  public void visitBinaryExpression(@NotNull JpqlBinaryExpression o) {
    visitExpression(o);
  }

  public void visitBooleanLiteral(@NotNull JpqlBooleanLiteral o) {
    visitExpression(o);
  }

  public void visitCaseOperand(@NotNull JpqlCaseOperand o) {
    visitPsiElement(o);
  }

  public void visitCoalesceExpression(@NotNull JpqlCoalesceExpression o) {
    visitExpression(o);
  }

  public void visitCollectionMemberDeclaration(@NotNull JpqlCollectionMemberDeclaration o) {
    visitPsiElement(o);
  }

  public void visitCollectionMemberExpression(@NotNull JpqlCollectionMemberExpression o) {
    visitExpression(o);
  }

  public void visitComparisonExpression(@NotNull JpqlComparisonExpression o) {
    visitExpression(o);
  }

  public void visitConditionalAndExpression(@NotNull JpqlConditionalAndExpression o) {
    visitBinaryExpression(o);
  }

  public void visitConditionalNotExpression(@NotNull JpqlConditionalNotExpression o) {
    visitExpression(o);
  }

  public void visitConditionalOrExpression(@NotNull JpqlConditionalOrExpression o) {
    visitBinaryExpression(o);
  }

  public void visitConstructorArgumentsList(@NotNull JpqlConstructorArgumentsList o) {
    visitPsiElement(o);
  }

  public void visitConstructorExpression(@NotNull JpqlConstructorExpression o) {
    visitExpression(o);
  }

  public void visitDatetimeFunctionExpression(@NotNull JpqlDatetimeFunctionExpression o) {
    visitExpression(o);
  }

  public void visitDatetimeLiteral(@NotNull JpqlDatetimeLiteral o) {
    visitExpression(o);
  }

  public void visitDeleteClause(@NotNull JpqlDeleteClause o) {
    visitPsiElement(o);
  }

  public void visitDeleteStatement(@NotNull JpqlDeleteStatement o) {
    visitPsiElement(o);
  }

  public void visitDerivedCollectionMemberDeclaration(@NotNull JpqlDerivedCollectionMemberDeclaration o) {
    visitPsiElement(o);
  }

  public void visitEmptyCollectionComparisonExpression(@NotNull JpqlEmptyCollectionComparisonExpression o) {
    visitExpression(o);
  }

  public void visitEntityAccess(@NotNull JpqlEntityAccess o) {
    visitPsiElement(o);
  }

  public void visitExistsExpression(@NotNull JpqlExistsExpression o) {
    visitExpression(o);
  }

  public void visitExpression(@NotNull JpqlExpression o) {
    visitPsiElement(o);
  }

  public void visitFetchCountOrPercent(@NotNull JpqlFetchCountOrPercent o) {
    visitPsiElement(o);
  }

  public void visitFetchClause(@NotNull JpqlFetchClause o) {
    visitPsiElement(o);
  }

  public void visitFromClause(@NotNull JpqlFromClause o) {
    visitAliasHost(o);
  }
    
    public void visitFullyQualifiedConstructor(@NotNull JpqlFullyQualifiedConstructor o) {
        visitReferenceExpression(o);
    }

  public void visitFunctionArg(@NotNull JpqlFunctionArg o) {
    visitPsiElement(o);
  }

  public void visitFunctionInvocationExpression(@NotNull JpqlFunctionInvocationExpression o) {
    visitExpression(o);
  }

  public void visitFunctionsReturningNumericsExpression(@NotNull JpqlFunctionsReturningNumericsExpression o) {
    visitExpression(o);
  }

  public void visitGeneralCaseExpression(@NotNull JpqlGeneralCaseExpression o) {
    visitExpression(o);
  }

  public void visitGroupbyClause(@NotNull JpqlGroupbyClause o) {
    visitPsiElement(o);
  }

  public void visitGroupbyItem(@NotNull JpqlGroupbyItem o) {
    visitPsiElement(o);
  }

  public void visitHavingClause(@NotNull JpqlHavingClause o) {
    visitPsiElement(o);
  }

  public void visitIdentificationVariableDeclaration(@NotNull JpqlIdentificationVariableDeclaration o) {
    visitPsiElement(o);
  }

  public void visitIdentifier(@NotNull JpqlIdentifier o) {
    visitNamedElement(o);
  }

  public void visitInExpression(@NotNull JpqlInExpression o) {
    visitExpression(o);
  }

  public void visitInItem(@NotNull JpqlInItem o) {
    visitPsiElement(o);
  }

  public void visitInputParameterExpression(@NotNull JpqlInputParameterExpression o) {
    visitExpression(o);
  }

  public void visitInsertFields(@NotNull JpqlInsertFields o) {
    visitPsiElement(o);
  }

  public void visitInsertStatement(@NotNull JpqlInsertStatement o) {
    visitPsiElement(o);
  }

  public void visitInsertTuple(@NotNull JpqlInsertTuple o) {
    visitPsiElement(o);
  }

  public void visitInsertValue(@NotNull JpqlInsertValue o) {
    visitPsiElement(o);
  }

  public void visitJoinCondition(@NotNull JpqlJoinCondition o) {
    visitPsiElement(o);
  }

  public void visitJoinExpression(@NotNull JpqlJoinExpression o) {
    visitExpression(o);
  }

  public void visitJoinSpec(@NotNull JpqlJoinSpec o) {
    visitPsiElement(o);
  }

  public void visitLikeExpression(@NotNull JpqlLikeExpression o) {
    visitExpression(o);
  }

  public void visitLimitClause(@NotNull JpqlLimitClause o) {
    visitPsiElement(o);
  }

  public void visitMapBasedReferenceExpression(@NotNull JpqlMapBasedReferenceExpression o) {
    visitReferenceExpression(o);
  }

  public void visitMultiplicativeExpression(@NotNull JpqlMultiplicativeExpression o) {
    visitBinaryExpression(o);
  }

  public void visitNullComparisonExpression(@NotNull JpqlNullComparisonExpression o) {
    visitExpression(o);
  }

  public void visitNullExpression(@NotNull JpqlNullExpression o) {
    visitExpression(o);
  }

  public void visitNullifExpression(@NotNull JpqlNullifExpression o) {
    visitExpression(o);
  }

  public void visitNumericLiteral(@NotNull JpqlNumericLiteral o) {
    visitExpression(o);
  }

  public void visitObjectExpression(@NotNull JpqlObjectExpression o) {
    visitExpression(o);
  }

  public void visitOffsetClause(@NotNull JpqlOffsetClause o) {
    visitPsiElement(o);
  }

  public void visitOrderbyClause(@NotNull JpqlOrderbyClause o) {
    visitPsiElement(o);
  }

  public void visitOrderbyItem(@NotNull JpqlOrderbyItem o) {
    visitPsiElement(o);
  }

  public void visitParenExpression(@NotNull JpqlParenExpression o) {
    visitExpression(o);
  }

  public void visitPathReferenceExpression(@NotNull JpqlPathReferenceExpression o) {
    visitReferenceExpression(o);
  }

  public void visitReferenceExpression(@NotNull JpqlReferenceExpression o) {
    visitExpression(o);
  }

  public void visitSelectClause(@NotNull JpqlSelectClause o) {
    visitPsiElement(o);
  }

  public void visitSelectItem(@NotNull JpqlSelectItem o) {
    visitPsiElement(o);
  }

  public void visitSelectStatement(@NotNull JpqlSelectStatement o) {
    visitPsiElement(o);
  }

  public void visitSimpleCaseExpression(@NotNull JpqlSimpleCaseExpression o) {
    visitExpression(o);
  }

  public void visitSimpleSelectClause(@NotNull JpqlSimpleSelectClause o) {
    visitPsiElement(o);
  }

  public void visitSimpleWhenClause(@NotNull JpqlSimpleWhenClause o) {
    visitPsiElement(o);
  }

  public void visitStatement(@NotNull JpqlStatement o) {
    visitPsiElement(o);
  }

  public void visitStringFunctionExpression(@NotNull JpqlStringFunctionExpression o) {
    visitExpression(o);
  }

  public void visitStringLiteral(@NotNull JpqlStringLiteral o) {
    visitExpression(o);
  }

  public void visitSubquery(@NotNull JpqlSubquery o) {
    visitPsiElement(o);
  }

  public void visitSubqueryExpression(@NotNull JpqlSubqueryExpression o) {
    visitExpression(o);
  }

  public void visitSubqueryFromClause(@NotNull JpqlSubqueryFromClause o) {
    visitAliasHost(o);
  }

  public void visitTrimSpecification(@NotNull JpqlTrimSpecification o) {
    visitPsiElement(o);
  }

  public void visitTypeExpression(@NotNull JpqlTypeExpression o) {
    visitExpression(o);
  }

  public void visitTypeLiteral(@NotNull JpqlTypeLiteral o) {
    visitPsiElement(o);
  }

  public void visitUnaryArithmeticExpression(@NotNull JpqlUnaryArithmeticExpression o) {
    visitExpression(o);
  }

  public void visitUpdateClause(@NotNull JpqlUpdateClause o) {
    visitPsiElement(o);
  }

  public void visitUpdateItem(@NotNull JpqlUpdateItem o) {
    visitPsiElement(o);
  }

  public void visitUpdateStatement(@NotNull JpqlUpdateStatement o) {
    visitPsiElement(o);
  }

  public void visitWhenClause(@NotNull JpqlWhenClause o) {
    visitPsiElement(o);
  }

  public void visitWhereClause(@NotNull JpqlWhereClause o) {
    visitPsiElement(o);
  }

  public void visitNameIdentifierOwner(@NotNull JpqlNameIdentifierOwner o) {
    visitPsiElement(o);
  }

  public void visitNamedElement(@NotNull JpqlNamedElement o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
