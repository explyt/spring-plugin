// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlEmptyCollectionComparisonExpression;
import com.explyt.jpa.ql.psi.JpqlReferenceExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlEmptyCollectionComparisonExpressionImpl extends JpqlExpressionImpl implements JpqlEmptyCollectionComparisonExpression {

  public JpqlEmptyCollectionComparisonExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitEmptyCollectionComparisonExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlReferenceExpression getReferenceExpression() {
    return findNotNullChildByClass(JpqlReferenceExpression.class);
  }

}
