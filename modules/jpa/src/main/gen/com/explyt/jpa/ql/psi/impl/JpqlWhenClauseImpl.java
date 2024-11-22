// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.explyt.jpa.ql.psi.JpqlWhenClause;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlWhenClauseImpl extends ASTWrapperPsiElement implements JpqlWhenClause {

  public JpqlWhenClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitWhenClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<JpqlExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlExpression.class);
  }

}
