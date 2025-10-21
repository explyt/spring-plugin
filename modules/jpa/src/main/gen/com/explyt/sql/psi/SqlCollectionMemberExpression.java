// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SqlCollectionMemberExpression extends SqlExpression {

    @NotNull
    List<SqlExpression> getExpressionList();

    @Nullable
    SqlIdentifier getIdentifier();

}
