// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface JpqlStatement extends PsiElement {

  @Nullable
  JpqlDeleteStatement getDeleteStatement();

  @Nullable
  JpqlInsertStatement getInsertStatement();

  @Nullable
  JpqlSelectStatement getSelectStatement();

  @Nullable
  JpqlUpdateStatement getUpdateStatement();

}
