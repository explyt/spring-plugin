// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlFunctionArg;
import com.explyt.jpa.ql.psi.JpqlFunctionInvocationExpression;
import com.explyt.jpa.ql.psi.JpqlStringLiteral;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JpqlFunctionInvocationExpressionImpl extends JpqlExpressionImpl implements JpqlFunctionInvocationExpression {

  public JpqlFunctionInvocationExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitFunctionInvocationExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<JpqlFunctionArg> getFunctionArgList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlFunctionArg.class);
  }

  @Override
  @Nullable
  public JpqlStringLiteral getStringLiteral() {
    return findChildByClass(JpqlStringLiteral.class);
  }

}
