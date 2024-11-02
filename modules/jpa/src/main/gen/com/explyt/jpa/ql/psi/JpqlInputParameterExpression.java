// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiPolyVariantReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JpqlInputParameterExpression extends JpqlExpression {

  @Nullable
  PsiElement getNamedInputParameter();

  @Nullable
  PsiElement getNumericInputParameter();

  @NotNull
  PsiPolyVariantReference getReference();

}
