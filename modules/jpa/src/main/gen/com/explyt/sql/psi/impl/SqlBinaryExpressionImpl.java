// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlBinaryExpression;
import com.explyt.sql.psi.SqlExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SqlBinaryExpressionImpl extends SqlExpressionImpl implements SqlBinaryExpression {

    public SqlBinaryExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitBinaryExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlExpression> getExpressionList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlExpression.class);
    }

    @Override
    @NotNull
    public SqlExpression getLeftOperand() {
        List<SqlExpression> p1 = getExpressionList();
        return p1.get(0);
    }

    @Override
    @Nullable
    public SqlExpression getRightOperand() {
        List<SqlExpression> p1 = getExpressionList();
        return p1.size() < 2 ? null : p1.get(1);
    }

}
