// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlIdentifier;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiPolyVariantReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.explyt.jpa.ql.psi.JpqlTypes.ID;

public class JpqlIdentifierImpl extends JpqlNamedElementImpl implements JpqlIdentifier {

  public JpqlIdentifierImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitIdentifier(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public PsiElement getId() {
    return findChildByType(ID);
  }

  @Override
  @NotNull
  public PsiElement setName(@NotNull String newName) {
    return JpqlPsiImplUtil.setName(this, newName);
  }

  @Override
  @NotNull
  public String getName() {
    return JpqlPsiImplUtil.getName(this);
  }

  @Override
  @Nullable
  public PsiPolyVariantReference getReference() {
    return JpqlPsiImplUtil.getReference(this);
  }

}
