// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface JpqlInsertStatement extends PsiElement {

  @NotNull
  JpqlEntityAccess getEntityAccess();

  @NotNull
  JpqlInsertFields getInsertFields();

  @NotNull
  List<JpqlInsertTuple> getInsertTupleList();

  @Nullable
  JpqlSelectStatement getSelectStatement();

}
