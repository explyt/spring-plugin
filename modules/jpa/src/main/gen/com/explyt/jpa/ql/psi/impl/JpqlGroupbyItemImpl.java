// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlGroupbyItem;
import com.explyt.jpa.ql.psi.JpqlIdentifier;
import com.explyt.jpa.ql.psi.JpqlReferenceExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlGroupbyItemImpl extends ASTWrapperPsiElement implements JpqlGroupbyItem {

  public JpqlGroupbyItemImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitGroupbyItem(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlIdentifier getIdentifier() {
    return findChildByClass(JpqlIdentifier.class);
  }

  @Override
  @Nullable
  public JpqlReferenceExpression getReferenceExpression() {
    return findChildByClass(JpqlReferenceExpression.class);
  }

}
