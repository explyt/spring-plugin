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

public class JpqlSelectStatementImpl extends ASTWrapperPsiElement implements JpqlSelectStatement {

  public JpqlSelectStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSelectStatement(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public JpqlFetchClause getFetchClause() {
    return findChildByClass(JpqlFetchClause.class);
  }

  @Override
  @NotNull
  public JpqlFromClause getFromClause() {
    return findNotNullChildByClass(JpqlFromClause.class);
  }

  @Override
  @Nullable
  public JpqlGroupbyClause getGroupbyClause() {
    return findChildByClass(JpqlGroupbyClause.class);
  }

  @Override
  @Nullable
  public JpqlHavingClause getHavingClause() {
    return findChildByClass(JpqlHavingClause.class);
  }

  @Override
  @Nullable
  public JpqlLimitClause getLimitClause() {
    return findChildByClass(JpqlLimitClause.class);
  }

  @Override
  @Nullable
  public JpqlOffsetClause getOffsetClause() {
    return findChildByClass(JpqlOffsetClause.class);
  }

  @Override
  @Nullable
  public JpqlOrderbyClause getOrderbyClause() {
    return findChildByClass(JpqlOrderbyClause.class);
  }

  @Override
  @Nullable
  public JpqlSelectClause getSelectClause() {
    return findChildByClass(JpqlSelectClause.class);
  }

  @Override
  @Nullable
  public JpqlWhereClause getWhereClause() {
    return findChildByClass(JpqlWhereClause.class);
  }

}
