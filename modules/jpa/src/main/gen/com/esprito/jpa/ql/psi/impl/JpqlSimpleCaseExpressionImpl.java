// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.esprito.jpa.ql.psi.JpqlTypes.*;
import com.esprito.jpa.ql.psi.*;

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
