// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class JpqlInsertStatementImpl extends ASTWrapperPsiElement implements JpqlInsertStatement {

  public JpqlInsertStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitInsertStatement(this);
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
  public JpqlInsertFields getInsertFields() {
    return findNotNullChildByClass(JpqlInsertFields.class);
  }

  @Override
  @NotNull
  public List<JpqlInsertTuple> getInsertTupleList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlInsertTuple.class);
  }

  @Override
  @Nullable
  public JpqlSelectStatement getSelectStatement() {
    return findChildByClass(JpqlSelectStatement.class);
  }

}
