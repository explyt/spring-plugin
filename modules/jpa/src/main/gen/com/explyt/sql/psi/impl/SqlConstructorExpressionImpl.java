// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlConstructorArgumentsList;
import com.explyt.sql.psi.SqlConstructorExpression;
import com.explyt.sql.psi.SqlFullyQualifiedConstructor;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class SqlConstructorExpressionImpl extends SqlExpressionImpl implements SqlConstructorExpression {

    public SqlConstructorExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitConstructorExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlConstructorArgumentsList getConstructorArgumentsList() {
        return findNotNullChildByClass(SqlConstructorArgumentsList.class);
    }

    @Override
    @NotNull
    public SqlFullyQualifiedConstructor getFullyQualifiedConstructor() {
        return findNotNullChildByClass(SqlFullyQualifiedConstructor.class);
    }

}
