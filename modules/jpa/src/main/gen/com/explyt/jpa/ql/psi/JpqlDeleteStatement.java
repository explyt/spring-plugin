// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JpqlDeleteStatement extends PsiElement {

  @NotNull
  JpqlDeleteClause getDeleteClause();

  @Nullable
  JpqlWhereClause getWhereClause();

}
