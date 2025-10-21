// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlStatementImpl extends ASTWrapperPsiElement implements SqlStatement {

    public SqlStatementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitStatement(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public SqlDeleteStatement getDeleteStatement() {
        return findChildByClass(SqlDeleteStatement.class);
    }

    @Override
    @Nullable
    public SqlInsertStatement getInsertStatement() {
        return findChildByClass(SqlInsertStatement.class);
    }

    @Override
    @Nullable
    public SqlSelectStatement getSelectStatement() {
        return findChildByClass(SqlSelectStatement.class);
    }

    @Override
    @Nullable
    public SqlUpdateStatement getUpdateStatement() {
        return findChildByClass(SqlUpdateStatement.class);
    }

}
