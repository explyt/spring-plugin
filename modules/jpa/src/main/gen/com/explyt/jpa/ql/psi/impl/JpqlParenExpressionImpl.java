// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExpression;
import com.explyt.jpa.ql.psi.JpqlParenExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlParenExpressionImpl extends JpqlExpressionImpl implements JpqlParenExpression {

  public JpqlParenExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitParenExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlExpression getExpression() {
    return findChildByClass(JpqlExpression.class);
  }

}
