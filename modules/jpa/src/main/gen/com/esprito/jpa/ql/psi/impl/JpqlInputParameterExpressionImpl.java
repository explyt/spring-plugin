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
  @NotNull
  public JpqlIdentifier getIdentifier() {
    return findNotNullChildByClass(JpqlIdentifier.class);
  }

}
