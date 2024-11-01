// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface JpqlJoinExpression extends JpqlExpression {

  @Nullable
  JpqlAliasDeclaration getAliasDeclaration();

  @Nullable
  JpqlJoinCondition getJoinCondition();

  @NotNull
  JpqlJoinSpec getJoinSpec();

  @Nullable
  JpqlReferenceExpression getReferenceExpression();

}
