// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlSimpleSelectExpression extends JpqlExpression {

  @Nullable
  JpqlDatetimeFunction getDatetimeFunction();

  @Nullable
  JpqlExpression getExpression();

  @Nullable
  JpqlIdentifier getIdentifier();

  @Nullable
  PsiElement getBooleanLiteral();

  @Nullable
  PsiElement getDatetimeLiteral();

  @Nullable
  PsiElement getStringLiteral();

}
