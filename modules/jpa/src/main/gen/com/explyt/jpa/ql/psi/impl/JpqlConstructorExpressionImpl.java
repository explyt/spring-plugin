// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlConstructorArgumentsList;
import com.explyt.jpa.ql.psi.JpqlConstructorExpression;
import com.explyt.jpa.ql.psi.JpqlFullyQualifiedConstructor;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlConstructorExpressionImpl extends JpqlExpressionImpl implements JpqlConstructorExpression {

  public JpqlConstructorExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitConstructorExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlConstructorArgumentsList getConstructorArgumentsList() {
    return findNotNullChildByClass(JpqlConstructorArgumentsList.class);
  }

  @Override
  @NotNull
  public JpqlFullyQualifiedConstructor getFullyQualifiedConstructor() {
      return findNotNullChildByClass(JpqlFullyQualifiedConstructor.class);
  }

}
