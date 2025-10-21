// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SqlFunctionInvocationExpression extends SqlExpression {

    @NotNull
    List<SqlFunctionArg> getFunctionArgList();

    @Nullable
    SqlStringLiteral getStringLiteral();

}
