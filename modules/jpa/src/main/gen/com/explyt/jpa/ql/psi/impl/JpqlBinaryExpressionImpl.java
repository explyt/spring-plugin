// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import java.util.List;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

import static com.explyt.jpa.ql.psi.JpqlTypes.*;

import com.explyt.jpa.ql.psi.*;

public class JpqlBinaryExpressionImpl extends JpqlExpressionImpl implements JpqlBinaryExpression {

  public JpqlBinaryExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitBinaryExpression(this);
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

  @Override
  @NotNull
  public JpqlExpression getLeftOperand() {
    List<JpqlExpression> p1 = getExpressionList();
    return p1.get(0);
  }

  @Override
  @Nullable
  public JpqlExpression getRightOperand() {
    List<JpqlExpression> p1 = getExpressionList();
    return p1.size() < 2 ? null : p1.get(1);
  }

}
