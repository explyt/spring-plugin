// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SqlBinaryExpression extends SqlExpression {

    @NotNull
    List<SqlExpression> getExpressionList();

    @NotNull
    SqlExpression getLeftOperand();

    @Nullable
    SqlExpression getRightOperand();

}
