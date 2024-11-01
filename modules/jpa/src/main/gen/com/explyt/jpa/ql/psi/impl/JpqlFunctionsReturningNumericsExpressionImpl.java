// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExpression;
import com.explyt.jpa.ql.psi.JpqlFunctionsReturningNumericsExpression;
import com.explyt.jpa.ql.psi.JpqlIdentifier;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlFunctionsReturningNumericsExpressionImpl extends JpqlExpressionImpl implements JpqlFunctionsReturningNumericsExpression {

  public JpqlFunctionsReturningNumericsExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitFunctionsReturningNumericsExpression(this);
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

  @Override
  @Nullable
  public JpqlIdentifier getIdentifier() {
    return findChildByClass(JpqlIdentifier.class);
  }

}
