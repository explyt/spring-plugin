// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SqlJoinExpression extends SqlExpression {

    @Nullable
    SqlAliasDeclaration getAliasDeclaration();

    @Nullable
    SqlExpression getExpression();

    @Nullable
    SqlJoinCondition getJoinCondition();

    @NotNull
    SqlJoinSpec getJoinSpec();

}
