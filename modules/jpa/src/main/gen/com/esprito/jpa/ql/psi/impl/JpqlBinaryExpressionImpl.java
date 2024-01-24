// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlBinaryExpression;
import com.esprito.jpa.ql.psi.JpqlExpression;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

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
