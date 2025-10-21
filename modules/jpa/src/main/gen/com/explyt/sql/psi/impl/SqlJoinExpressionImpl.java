// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlJoinExpressionImpl extends SqlExpressionImpl implements SqlJoinExpression {

    public SqlJoinExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitJoinExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public SqlAliasDeclaration getAliasDeclaration() {
        return findChildByClass(SqlAliasDeclaration.class);
    }

    @Override
    @Nullable
    public SqlExpression getExpression() {
        return findChildByClass(SqlExpression.class);
    }

    @Override
    @Nullable
    public SqlJoinCondition getJoinCondition() {
        return findChildByClass(SqlJoinCondition.class);
    }

    @Override
    @NotNull
    public SqlJoinSpec getJoinSpec() {
        return findNotNullChildByClass(SqlJoinSpec.class);
    }

}
