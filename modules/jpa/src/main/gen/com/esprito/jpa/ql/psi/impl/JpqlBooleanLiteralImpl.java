// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlBooleanLiteral;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import static com.esprito.jpa.ql.psi.JpqlTypes.BOOLEAN;

public class JpqlBooleanLiteralImpl extends JpqlExpressionImpl implements JpqlBooleanLiteral {

  public JpqlBooleanLiteralImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitBooleanLiteral(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getBoolean() {
    return findNotNullChildByType(BOOLEAN);
  }

  @Override
  public boolean getValue() {
    return JpqlPsiImplUtil.getValue(this);
  }

}
