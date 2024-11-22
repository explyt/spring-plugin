// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface JpqlUpdateClause extends PsiElement {

  @NotNull
  JpqlEntityAccess getEntityAccess();

  @NotNull
  List<JpqlUpdateItem> getUpdateItemList();

}
