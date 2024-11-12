// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlAliasDeclaration;
import com.explyt.jpa.ql.psi.JpqlEntityAccess;
import com.explyt.jpa.ql.psi.JpqlIdentifier;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlEntityAccessImpl extends ASTWrapperPsiElement implements JpqlEntityAccess {

  public JpqlEntityAccessImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitEntityAccess(this);
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
  public JpqlIdentifier getIdentifier() {
    return findNotNullChildByClass(JpqlIdentifier.class);
  }

}
