// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlStatementImpl extends ASTWrapperPsiElement implements JpqlStatement {

  public JpqlStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlDeleteStatement getDeleteStatement() {
    return findChildByClass(JpqlDeleteStatement.class);
  }

  @Override
  @Nullable
  public JpqlInsertStatement getInsertStatement() {
    return findChildByClass(JpqlInsertStatement.class);
  }

  @Override
  @Nullable
  public JpqlSelectStatement getSelectStatement() {
    return findChildByClass(JpqlSelectStatement.class);
  }

  @Override
  @Nullable
  public JpqlUpdateStatement getUpdateStatement() {
    return findChildByClass(JpqlUpdateStatement.class);
  }

}
