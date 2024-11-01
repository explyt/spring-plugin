// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlDeleteClause;
import com.explyt.jpa.ql.psi.JpqlDeleteStatement;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.explyt.jpa.ql.psi.JpqlWhereClause;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlDeleteStatementImpl extends ASTWrapperPsiElement implements JpqlDeleteStatement {

  public JpqlDeleteStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitDeleteStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlDeleteClause getDeleteClause() {
    return findNotNullChildByClass(JpqlDeleteClause.class);
  }

  @Override
  @Nullable
  public JpqlWhereClause getWhereClause() {
    return findChildByClass(JpqlWhereClause.class);
  }

}
