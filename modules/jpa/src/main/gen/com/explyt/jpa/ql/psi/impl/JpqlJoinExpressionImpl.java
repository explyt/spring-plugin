// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlJoinExpressionImpl extends JpqlExpressionImpl implements JpqlJoinExpression {

  public JpqlJoinExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitJoinExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlAliasDeclaration getAliasDeclaration() {
    return findChildByClass(JpqlAliasDeclaration.class);
  }

  @Override
  @Nullable
  public JpqlJoinCondition getJoinCondition() {
    return findChildByClass(JpqlJoinCondition.class);
  }

  @Override
  @NotNull
  public JpqlJoinSpec getJoinSpec() {
    return findNotNullChildByClass(JpqlJoinSpec.class);
  }

  @Override
  @Nullable
  public JpqlReferenceExpression getReferenceExpression() {
    return findChildByClass(JpqlReferenceExpression.class);
  }

}
