// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlExpression;
import com.esprito.jpa.ql.psi.JpqlIdentifier;
import com.esprito.jpa.ql.psi.JpqlInItem;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlInItemImpl extends ASTWrapperPsiElement implements JpqlInItem {

  public JpqlInItemImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitInItem(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
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

}
