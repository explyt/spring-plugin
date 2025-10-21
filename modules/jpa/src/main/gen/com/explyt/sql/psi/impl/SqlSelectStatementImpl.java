// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlSelectStatementImpl extends ASTWrapperPsiElement implements SqlSelectStatement {

    public SqlSelectStatementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitSelectStatement(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlFromClause getFromClause() {
        return findNotNullChildByClass(SqlFromClause.class);
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
    @Nullable
    public SqlLimitClause getLimitClause() {
        return findChildByClass(SqlLimitClause.class);
    }

    @Override
    @Nullable
    public SqlOffsetClause getOffsetClause() {
        return findChildByClass(SqlOffsetClause.class);
    }

    @Override
    @Nullable
    public SqlOrderbyClause getOrderbyClause() {
        return findChildByClass(SqlOrderbyClause.class);
    }

    @Override
    @NotNull
    public SqlSelectClause getSelectClause() {
        return findNotNullChildByClass(SqlSelectClause.class);
    }

    @Override
    @Nullable
    public SqlWhereClause getWhereClause() {
        return findChildByClass(SqlWhereClause.class);
    }

}
