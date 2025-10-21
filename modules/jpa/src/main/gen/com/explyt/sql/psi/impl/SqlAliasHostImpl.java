// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlAliasHost;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlAliasHostImpl extends ASTWrapperPsiElement implements SqlAliasHost {

    public SqlAliasHostImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitAliasHost(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

}
