// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SqlIdentificationVariableDeclaration extends PsiElement {

    @NotNull
    List<SqlJoinExpression> getJoinExpressionList();

    @NotNull
    SqlTableNameRef getTableNameRef();

}
