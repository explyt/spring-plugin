// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExistsExpression;
import com.explyt.jpa.ql.psi.JpqlSubquery;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlExistsExpressionImpl extends JpqlExpressionImpl implements JpqlExistsExpression {

  public JpqlExistsExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitExistsExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlSubquery getSubquery() {
    return findNotNullChildByClass(JpqlSubquery.class);
  }

}
