// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExpression;
import com.explyt.jpa.ql.psi.JpqlUpdateItem;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlUpdateItemImpl extends ASTWrapperPsiElement implements JpqlUpdateItem {

  public JpqlUpdateItemImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitUpdateItem(this);
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
