// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlEntityAccess;
import com.explyt.jpa.ql.psi.JpqlIdentificationVariableDeclaration;
import com.explyt.jpa.ql.psi.JpqlJoinExpression;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlIdentificationVariableDeclarationImpl extends ASTWrapperPsiElement implements JpqlIdentificationVariableDeclaration {

  public JpqlIdentificationVariableDeclarationImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitIdentificationVariableDeclaration(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlEntityAccess getEntityAccess() {
    return findNotNullChildByClass(JpqlEntityAccess.class);
  }

  @Override
  @NotNull
  public List<JpqlJoinExpression> getJoinExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlJoinExpression.class);
  }

}
