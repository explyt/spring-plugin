// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.esprito.jpql.psi.JpqlTypes.*;
import com.esprito.jpql.psi.*;

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
  @Nullable
  public JpqlDatetimeFunction getDatetimeFunction() {
    return findChildByClass(JpqlDatetimeFunction.class);
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

  @Override
  @NotNull
  public List<JpqlSimpleWhenClause> getSimpleWhenClauseList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlSimpleWhenClause.class);
  }

  @Override
  @Nullable
  public PsiElement getBooleanLiteral() {
    return findChildByType(BOOLEAN_LITERAL);
  }

  @Override
  @Nullable
  public PsiElement getDatetimeLiteral() {
    return findChildByType(DATETIME_LITERAL);
  }

  @Override
  @Nullable
  public PsiElement getStringLiteral() {
    return findChildByType(STRING_LITERAL);
  }

}
