// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface SqlInsertStatement extends PsiElement {

    @NotNull
    SqlInsertFields getInsertFields();

    @NotNull
    List<SqlInsertTuple> getInsertTupleList();

    @Nullable
    SqlSelectStatement getSelectStatement();

    @NotNull
    SqlTableNameRef getTableNameRef();

}
