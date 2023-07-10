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

public class JpqlDatetimeFunctionExpressionImpl extends JpqlExpressionImpl implements JpqlDatetimeFunctionExpression {

  public JpqlDatetimeFunctionExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitDatetimeFunctionExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlDatetimeFunction getDatetimeFunction() {
    return findNotNullChildByClass(JpqlDatetimeFunction.class);
  }

}
