// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlComparisonExpression;
import com.esprito.jpa.ql.psi.JpqlExpression;
import com.esprito.jpa.ql.psi.JpqlTokenType;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlComparisonExpressionImpl extends JpqlExpressionImpl implements JpqlComparisonExpression {

  public JpqlComparisonExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitComparisonExpression(this);
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
  public JpqlTokenType getOperator() {
    return JpqlPsiImplUtil.getOperator(this);
  }

}
