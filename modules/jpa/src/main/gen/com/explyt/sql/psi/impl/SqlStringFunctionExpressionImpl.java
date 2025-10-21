// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlExpression;
import com.explyt.sql.psi.SqlStringFunctionExpression;
import com.explyt.sql.psi.SqlTrimSpecification;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class SqlStringFunctionExpressionImpl extends SqlExpressionImpl implements SqlStringFunctionExpression {

    public SqlStringFunctionExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitStringFunctionExpression(this);
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
    @Nullable
    public SqlTrimSpecification getTrimSpecification() {
        return findChildByClass(SqlTrimSpecification.class);
    }

}
