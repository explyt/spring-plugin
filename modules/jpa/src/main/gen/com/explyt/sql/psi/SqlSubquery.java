// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SqlSubquery extends PsiElement {

    @Nullable
    SqlGroupbyClause getGroupbyClause();

    @Nullable
    SqlHavingClause getHavingClause();

    @NotNull
    SqlSimpleSelectClause getSimpleSelectClause();

    @NotNull
    SqlSubqueryFromClause getSubqueryFromClause();

    @Nullable
    SqlWhereClause getWhereClause();

}
