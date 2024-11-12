// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JpqlSelectStatement extends PsiElement {

  @Nullable
  JpqlFetchClause getFetchClause();

  @NotNull
  JpqlFromClause getFromClause();

  @Nullable
  JpqlGroupbyClause getGroupbyClause();

  @Nullable
  JpqlHavingClause getHavingClause();

  @Nullable
  JpqlLimitClause getLimitClause();

  @Nullable
  JpqlOffsetClause getOffsetClause();

  @Nullable
  JpqlOrderbyClause getOrderbyClause();

  @Nullable
  JpqlSelectClause getSelectClause();

  @Nullable
  JpqlWhereClause getWhereClause();

}
