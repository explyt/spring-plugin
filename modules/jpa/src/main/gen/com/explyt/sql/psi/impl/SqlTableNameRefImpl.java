// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlAliasDeclaration;
import com.explyt.sql.psi.SqlIdentifier;
import com.explyt.sql.psi.SqlTableNameRef;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlTableNameRefImpl extends ASTWrapperPsiElement implements SqlTableNameRef {

    public SqlTableNameRefImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitTableNameRef(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public SqlAliasDeclaration getAliasDeclaration() {
        return findChildByClass(SqlAliasDeclaration.class);
    }

    @Override
    @NotNull
    public SqlIdentifier getIdentifier() {
        return findNotNullChildByClass(SqlIdentifier.class);
    }

}
