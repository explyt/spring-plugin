// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlUpdateClause;
import com.explyt.jpa.ql.psi.JpqlUpdateStatement;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.explyt.jpa.ql.psi.JpqlWhereClause;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JpqlUpdateStatementImpl extends ASTWrapperPsiElement implements JpqlUpdateStatement {

  public JpqlUpdateStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitUpdateStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlUpdateClause getUpdateClause() {
    return findNotNullChildByClass(JpqlUpdateClause.class);
  }

  @Override
  @Nullable
  public JpqlWhereClause getWhereClause() {
    return findChildByClass(JpqlWhereClause.class);
  }

}
