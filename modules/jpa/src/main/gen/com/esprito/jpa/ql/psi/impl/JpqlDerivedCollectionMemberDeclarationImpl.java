// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlDerivedCollectionMemberDeclaration;
import com.esprito.jpa.ql.psi.JpqlReferenceExpression;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class JpqlDerivedCollectionMemberDeclarationImpl extends ASTWrapperPsiElement implements JpqlDerivedCollectionMemberDeclaration {

  public JpqlDerivedCollectionMemberDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitDerivedCollectionMemberDeclaration(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlReferenceExpression getReferenceExpression() {
    return findNotNullChildByClass(JpqlReferenceExpression.class);
  }

}
