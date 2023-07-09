// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlInItem extends PsiElement {

  @Nullable
  JpqlIdentifier getIdentifier();

  @Nullable
  JpqlInputParameterExpression getInputParameterExpression();

  @Nullable
  PsiElement getBooleanLiteral();

  @Nullable
  PsiElement getDatetimeLiteral();

  @Nullable
  PsiElement getNumericLiteral();

  @Nullable
  PsiElement getStringLiteral();

}
