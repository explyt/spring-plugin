// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlExpression;
import com.explyt.jpa.ql.psi.JpqlGeneralCaseExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.explyt.jpa.ql.psi.JpqlWhenClause;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlGeneralCaseExpressionImpl extends JpqlExpressionImpl implements JpqlGeneralCaseExpression {

  public JpqlGeneralCaseExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitGeneralCaseExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlExpression getExpression() {
    return findNotNullChildByClass(JpqlExpression.class);
  }

  @Override
  @NotNull
  public List<JpqlWhenClause> getWhenClauseList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlWhenClause.class);
  }

}
