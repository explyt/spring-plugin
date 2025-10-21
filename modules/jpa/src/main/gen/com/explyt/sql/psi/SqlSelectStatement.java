// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SqlSelectStatement extends PsiElement {

    @NotNull
    SqlFromClause getFromClause();

    @Nullable
    SqlGroupbyClause getGroupbyClause();

    @Nullable
    SqlHavingClause getHavingClause();

    @Nullable
    SqlLimitClause getLimitClause();

    @Nullable
    SqlOffsetClause getOffsetClause();

    @Nullable
    SqlOrderbyClause getOrderbyClause();

    @NotNull
    SqlSelectClause getSelectClause();

    @Nullable
    SqlWhereClause getWhereClause();

}
