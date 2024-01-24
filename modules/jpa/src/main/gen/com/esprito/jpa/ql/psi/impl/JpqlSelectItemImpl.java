// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlAliasDeclaration;
import com.esprito.jpa.ql.psi.JpqlExpression;
import com.esprito.jpa.ql.psi.JpqlSelectItem;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlSelectItemImpl extends ASTWrapperPsiElement implements JpqlSelectItem {

  public JpqlSelectItemImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSelectItem(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlAliasDeclaration getAliasDeclaration() {
    return findChildByClass(JpqlAliasDeclaration.class);
  }

  @Override
  @NotNull
  public JpqlExpression getExpression() {
    return findNotNullChildByClass(JpqlExpression.class);
  }

}
