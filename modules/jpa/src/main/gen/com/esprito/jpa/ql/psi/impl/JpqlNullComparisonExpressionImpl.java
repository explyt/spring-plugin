// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlExpression;
import com.esprito.jpa.ql.psi.JpqlNullComparisonExpression;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlNullComparisonExpressionImpl extends JpqlExpressionImpl implements JpqlNullComparisonExpression {

  public JpqlNullComparisonExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitNullComparisonExpression(this);
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
