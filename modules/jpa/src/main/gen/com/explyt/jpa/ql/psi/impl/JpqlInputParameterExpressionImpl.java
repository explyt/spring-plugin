// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlInputParameterExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiPolyVariantReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.explyt.jpa.ql.psi.JpqlTypes.NAMED_INPUT_PARAMETER;
import static com.explyt.jpa.ql.psi.JpqlTypes.NUMERIC_INPUT_PARAMETER;

public class JpqlInputParameterExpressionImpl extends JpqlExpressionImpl implements JpqlInputParameterExpression {

  public JpqlInputParameterExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitInputParameterExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getNamedInputParameter() {
    return findChildByType(NAMED_INPUT_PARAMETER);
  }

  @Override
  @Nullable
  public PsiElement getNumericInputParameter() {
    return findChildByType(NUMERIC_INPUT_PARAMETER);
  }

  @Override
  @NotNull
  public PsiPolyVariantReference getReference() {
    return JpqlPsiImplUtil.getReference(this);
  }

}
