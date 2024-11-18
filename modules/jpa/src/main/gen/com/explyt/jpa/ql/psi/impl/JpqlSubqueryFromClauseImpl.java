// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlSubqueryFromClauseImpl extends JpqlAliasHostImpl implements JpqlSubqueryFromClause {

  public JpqlSubqueryFromClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSubqueryFromClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<JpqlAliasDeclaration> getAliasDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlAliasDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlCollectionMemberDeclaration> getCollectionMemberDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlCollectionMemberDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlDerivedCollectionMemberDeclaration> getDerivedCollectionMemberDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlDerivedCollectionMemberDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlExpression.class);
  }

  @Override
  @NotNull
  public List<JpqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlIdentificationVariableDeclaration.class);
  }

}
