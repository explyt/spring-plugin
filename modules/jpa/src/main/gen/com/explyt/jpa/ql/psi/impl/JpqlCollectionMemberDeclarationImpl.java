// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import java.util.List;

import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

import static com.explyt.jpa.ql.psi.JpqlTypes.*;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.explyt.jpa.ql.psi.*;

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
