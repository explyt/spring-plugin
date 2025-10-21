// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SqlInExpression extends SqlExpression {

    @NotNull
    List<SqlExpression> getExpressionList();

    @NotNull
    List<SqlInItem> getInItemList();

    @Nullable
    SqlSubquery getSubquery();

}
