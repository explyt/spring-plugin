// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

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
