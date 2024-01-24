// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlAliasDeclaration;
import com.esprito.jpa.ql.psi.JpqlCollectionMemberDeclaration;
import com.esprito.jpa.ql.psi.JpqlReferenceExpression;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlCollectionMemberDeclarationImpl extends ASTWrapperPsiElement implements JpqlCollectionMemberDeclaration {

  public JpqlCollectionMemberDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitCollectionMemberDeclaration(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlAliasDeclaration getAliasDeclaration() {
    return findNotNullChildByClass(JpqlAliasDeclaration.class);
  }

  @Override
  @NotNull
  public JpqlReferenceExpression getReferenceExpression() {
    return findNotNullChildByClass(JpqlReferenceExpression.class);
  }

}
