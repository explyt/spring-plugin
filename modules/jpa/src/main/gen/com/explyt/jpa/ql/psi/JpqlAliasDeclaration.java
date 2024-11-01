// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JpqlAliasDeclaration extends JpqlNameIdentifierOwner {

  @NotNull
  JpqlIdentifier getIdentifier();

  @NotNull
  PsiElement setName(@NotNull String newName);

  @NotNull
  String getName();

  @Nullable
  PsiElement getReferencedElement();

  @Nullable
  PsiElement getNameIdentifier();

}
