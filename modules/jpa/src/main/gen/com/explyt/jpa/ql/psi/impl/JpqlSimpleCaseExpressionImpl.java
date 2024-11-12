// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlSimpleCaseExpressionImpl extends JpqlExpressionImpl implements JpqlSimpleCaseExpression {

  public JpqlSimpleCaseExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSimpleCaseExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlCaseOperand getCaseOperand() {
    return findNotNullChildByClass(JpqlCaseOperand.class);
  }

  @Override
  @NotNull
  public JpqlExpression getExpression() {
    return findNotNullChildByClass(JpqlExpression.class);
  }

  @Override
  @NotNull
  public List<JpqlSimpleWhenClause> getSimpleWhenClauseList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlSimpleWhenClause.class);
  }

}
