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

public class JpqlSubqueryImpl extends ASTWrapperPsiElement implements JpqlSubquery {

  public JpqlSubqueryImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSubquery(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
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
  @NotNull
  public JpqlSimpleSelectClause getSimpleSelectClause() {
    return findNotNullChildByClass(JpqlSimpleSelectClause.class);
  }

  @Override
  @NotNull
  public JpqlSubqueryFromClause getSubqueryFromClause() {
    return findNotNullChildByClass(JpqlSubqueryFromClause.class);
  }

  @Override
  @Nullable
  public JpqlWhereClause getWhereClause() {
    return findChildByClass(JpqlWhereClause.class);
  }

}
