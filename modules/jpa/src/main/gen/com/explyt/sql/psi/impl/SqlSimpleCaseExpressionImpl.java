// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlSimpleCaseExpressionImpl extends SqlExpressionImpl implements SqlSimpleCaseExpression {

    public SqlSimpleCaseExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitSimpleCaseExpression(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public SqlCaseOperand getCaseOperand() {
        return findNotNullChildByClass(SqlCaseOperand.class);
    }

    @Override
    @NotNull
    public SqlExpression getExpression() {
        return findNotNullChildByClass(SqlExpression.class);
    }

    @Override
    @NotNull
    public List<SqlSimpleWhenClause> getSimpleWhenClauseList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlSimpleWhenClause.class);
    }

}
