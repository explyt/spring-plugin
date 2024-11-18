// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface JpqlSimpleCaseExpression extends JpqlExpression {

  @NotNull
  JpqlCaseOperand getCaseOperand();

  @NotNull
  JpqlExpression getExpression();

  @NotNull
  List<JpqlSimpleWhenClause> getSimpleWhenClauseList();

}
