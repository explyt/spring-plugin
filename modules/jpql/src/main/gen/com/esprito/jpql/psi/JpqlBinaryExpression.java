// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlBinaryExpression extends JpqlExpression {

  @NotNull
  List<JpqlExpression> getExpressionList();

  @NotNull
  JpqlExpression getLeftOperand();

  @Nullable
  JpqlExpression getRightOperand();

}
