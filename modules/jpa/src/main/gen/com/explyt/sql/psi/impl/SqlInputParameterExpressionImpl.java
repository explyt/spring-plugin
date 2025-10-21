// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlInputParameterExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.explyt.sql.psi.SqlTypes.NAMED_INPUT_PARAMETER;
import static com.explyt.sql.psi.SqlTypes.NUMERIC_INPUT_PARAMETER;

public class SqlInputParameterExpressionImpl extends SqlExpressionImpl implements SqlInputParameterExpression {

    public SqlInputParameterExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitInputParameterExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @Nullable
    public PsiElement getNamedInputParameter() {
        return findChildByType(NAMED_INPUT_PARAMETER);
    }

    @Override
    @Nullable
    public PsiElement getNumericInputParameter() {
        return findChildByType(NUMERIC_INPUT_PARAMETER);
    }

}
