// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlUpdateClause;
import com.explyt.sql.psi.SqlUpdateStatement;
import com.explyt.sql.psi.SqlVisitor;
import com.explyt.sql.psi.SqlWhereClause;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlUpdateStatementImpl extends ASTWrapperPsiElement implements SqlUpdateStatement {

    public SqlUpdateStatementImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitUpdateStatement(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlUpdateClause getUpdateClause() {
        return findNotNullChildByClass(SqlUpdateClause.class);
    }

    @Override
    @Nullable
    public SqlWhereClause getWhereClause() {
        return findChildByClass(SqlWhereClause.class);
    }

}
