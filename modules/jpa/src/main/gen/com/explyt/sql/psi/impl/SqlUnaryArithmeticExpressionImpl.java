// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlExpression;
import com.explyt.sql.psi.SqlUnaryArithmeticExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlUnaryArithmeticExpressionImpl extends SqlExpressionImpl implements SqlUnaryArithmeticExpression {

    public SqlUnaryArithmeticExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitUnaryArithmeticExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public SqlExpression getExpression() {
        return findChildByClass(SqlExpression.class);
    }

}
