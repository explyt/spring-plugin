// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlDatetimeFunctionExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlDatetimeFunctionExpressionImpl extends SqlExpressionImpl implements SqlDatetimeFunctionExpression {

    public SqlDatetimeFunctionExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitDatetimeFunctionExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

}
