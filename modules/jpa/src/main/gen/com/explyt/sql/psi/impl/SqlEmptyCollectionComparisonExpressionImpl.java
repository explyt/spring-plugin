// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlEmptyCollectionComparisonExpression;
import com.explyt.sql.psi.SqlExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlEmptyCollectionComparisonExpressionImpl extends SqlExpressionImpl implements SqlEmptyCollectionComparisonExpression {

    public SqlEmptyCollectionComparisonExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitEmptyCollectionComparisonExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlExpression getExpression() {
        return findNotNullChildByClass(SqlExpression.class);
    }

}
