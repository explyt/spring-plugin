// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlMapBasedReferenceExpression;
import com.explyt.sql.psi.SqlReferenceExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlMapBasedReferenceExpressionImpl extends SqlReferenceExpressionImpl implements SqlMapBasedReferenceExpression {

    public SqlMapBasedReferenceExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitMapBasedReferenceExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlReferenceExpression getReferenceExpression() {
        return findNotNullChildByClass(SqlReferenceExpression.class);
    }

}
