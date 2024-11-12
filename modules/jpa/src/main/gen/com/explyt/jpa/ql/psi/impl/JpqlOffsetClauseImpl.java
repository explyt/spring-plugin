// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExpression;
import com.explyt.jpa.ql.psi.JpqlOffsetClause;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlOffsetClauseImpl extends ASTWrapperPsiElement implements JpqlOffsetClause {

  public JpqlOffsetClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitOffsetClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlExpression getExpression() {
    return findNotNullChildByClass(JpqlExpression.class);
  }

}
