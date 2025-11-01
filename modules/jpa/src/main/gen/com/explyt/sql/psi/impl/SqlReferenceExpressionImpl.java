// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlAsterisk;
import com.explyt.sql.psi.SqlReferenceExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlReferenceExpressionImpl extends SqlExpressionImpl implements SqlReferenceExpression {

    public SqlReferenceExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitReferenceExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public SqlAsterisk getAsterisk() {
        return findChildByClass(SqlAsterisk.class);
    }

}
