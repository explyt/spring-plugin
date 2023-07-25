// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

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
