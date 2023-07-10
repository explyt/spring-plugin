// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlJoinExpression extends JpqlExpression {

  @NotNull
  JpqlIdentifier getIdentifier();

  @Nullable
  JpqlJoinCondition getJoinCondition();

  @NotNull
  JpqlJoinSpec getJoinSpec();

  @NotNull
  JpqlReferenceExpression getReferenceExpression();

}
