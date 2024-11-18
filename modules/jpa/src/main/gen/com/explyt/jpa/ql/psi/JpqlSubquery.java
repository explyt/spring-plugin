// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JpqlSubquery extends PsiElement {

  @Nullable
  JpqlGroupbyClause getGroupbyClause();

  @Nullable
  JpqlHavingClause getHavingClause();

  @NotNull
  JpqlSimpleSelectClause getSimpleSelectClause();

  @NotNull
  JpqlSubqueryFromClause getSubqueryFromClause();

  @Nullable
  JpqlWhereClause getWhereClause();

}
