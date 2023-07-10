// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

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
