// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlSubqueryImpl extends ASTWrapperPsiElement implements JpqlSubquery {

  public JpqlSubqueryImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSubquery(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlGroupbyClause getGroupbyClause() {
    return findChildByClass(JpqlGroupbyClause.class);
  }

  @Override
  @Nullable
  public JpqlHavingClause getHavingClause() {
    return findChildByClass(JpqlHavingClause.class);
  }

  @Override
  @NotNull
  public JpqlSimpleSelectClause getSimpleSelectClause() {
    return findNotNullChildByClass(JpqlSimpleSelectClause.class);
  }

  @Override
  @NotNull
  public JpqlSubqueryFromClause getSubqueryFromClause() {
    return findNotNullChildByClass(JpqlSubqueryFromClause.class);
  }

  @Override
  @Nullable
  public JpqlWhereClause getWhereClause() {
    return findChildByClass(JpqlWhereClause.class);
  }

}
