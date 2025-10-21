// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlAliasDeclaration;
import com.explyt.sql.psi.SqlCollectionMemberDeclaration;
import com.explyt.sql.psi.SqlExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlCollectionMemberDeclarationImpl extends ASTWrapperPsiElement implements SqlCollectionMemberDeclaration {

    public SqlCollectionMemberDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitCollectionMemberDeclaration(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlAliasDeclaration getAliasDeclaration() {
        return findNotNullChildByClass(SqlAliasDeclaration.class);
    }

    @Override
    @NotNull
    public SqlExpression getExpression() {
        return findNotNullChildByClass(SqlExpression.class);
    }

}
