// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SqlInExpressionImpl extends SqlExpressionImpl implements SqlInExpression {

    public SqlInExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitInExpression(this);
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
    public List<SqlInItem> getInItemList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlInItem.class);
    }

    @Override
    @Nullable
    public SqlSubquery getSubquery() {
        return findChildByClass(SqlSubquery.class);
    }

}
