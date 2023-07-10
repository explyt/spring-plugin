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
