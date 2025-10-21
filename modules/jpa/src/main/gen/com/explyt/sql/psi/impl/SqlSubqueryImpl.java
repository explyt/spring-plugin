// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlSubqueryImpl extends ASTWrapperPsiElement implements SqlSubquery {

    public SqlSubqueryImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitSubquery(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public SqlGroupbyClause getGroupbyClause() {
        return findChildByClass(SqlGroupbyClause.class);
    }

    @Override
    @Nullable
    public SqlHavingClause getHavingClause() {
        return findChildByClass(SqlHavingClause.class);
    }

    @Override
    @NotNull
    public SqlSimpleSelectClause getSimpleSelectClause() {
        return findNotNullChildByClass(SqlSimpleSelectClause.class);
    }

    @Override
    @NotNull
    public SqlSubqueryFromClause getSubqueryFromClause() {
        return findNotNullChildByClass(SqlSubqueryFromClause.class);
    }

    @Override
    @Nullable
    public SqlWhereClause getWhereClause() {
        return findChildByClass(SqlWhereClause.class);
    }

}
