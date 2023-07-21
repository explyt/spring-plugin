// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.esprito.jpa.ql.psi.JpqlTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.esprito.jpa.ql.psi.*;

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
