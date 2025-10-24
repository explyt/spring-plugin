// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.Nullable;

public interface SqlStatement extends PsiElement {

    @Nullable
    SqlDeleteStatement getDeleteStatement();

    @Nullable
    SqlInsertStatement getInsertStatement();

    @Nullable
    SqlSelectStatement getSelectStatement();

    @Nullable
    SqlUpdateStatement getUpdateStatement();

}
