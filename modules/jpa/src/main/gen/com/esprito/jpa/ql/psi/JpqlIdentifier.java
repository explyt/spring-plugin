// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;

public interface JpqlIdentifier extends JpqlNamedElement {

  @Nullable
  PsiElement getId();

  @NotNull
  PsiElement setName(@NotNull String newName);

  @NotNull
  String getName();

  @Nullable
  PsiPolyVariantReference getReference();

}
