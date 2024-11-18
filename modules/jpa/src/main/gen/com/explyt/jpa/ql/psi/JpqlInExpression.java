// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface JpqlInExpression extends JpqlExpression {

  @NotNull
  List<JpqlExpression> getExpressionList();

  @NotNull
  List<JpqlInItem> getInItemList();

  @Nullable
  JpqlSubquery getSubquery();

}
