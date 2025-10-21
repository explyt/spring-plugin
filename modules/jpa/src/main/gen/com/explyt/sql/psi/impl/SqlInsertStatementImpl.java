// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SqlInsertStatementImpl extends ASTWrapperPsiElement implements SqlInsertStatement {

    public SqlInsertStatementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitInsertStatement(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlInsertFields getInsertFields() {
        return findNotNullChildByClass(SqlInsertFields.class);
    }

    @Override
    @NotNull
    public List<SqlInsertTuple> getInsertTupleList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlInsertTuple.class);
    }

    @Override
    @Nullable
    public SqlSelectStatement getSelectStatement() {
        return findChildByClass(SqlSelectStatement.class);
    }

    @Override
    @NotNull
    public SqlTableNameRef getTableNameRef() {
        return findNotNullChildByClass(SqlTableNameRef.class);
    }

}
