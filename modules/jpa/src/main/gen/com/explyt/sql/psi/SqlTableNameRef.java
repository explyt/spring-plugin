// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface SqlTableNameRef extends PsiElement {

    @Nullable
    SqlAliasDeclaration getAliasDeclaration();

    @NotNull
    SqlIdentifier getIdentifier();

}
