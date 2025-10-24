// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlAliasDeclaration;
import com.explyt.sql.psi.SqlExpression;
import com.explyt.sql.psi.SqlSelectItem;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlSelectItemImpl extends ASTWrapperPsiElement implements SqlSelectItem {

    public SqlSelectItemImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitSelectItem(this);
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
    public SqlExpression getExpression() {
        return findNotNullChildByClass(SqlExpression.class);
    }

}
