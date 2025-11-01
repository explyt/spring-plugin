// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SqlGeneralCaseExpression extends SqlExpression {

    @NotNull
    SqlExpression getExpression();

    @NotNull
    List<SqlWhenClause> getWhenClauseList();

}
