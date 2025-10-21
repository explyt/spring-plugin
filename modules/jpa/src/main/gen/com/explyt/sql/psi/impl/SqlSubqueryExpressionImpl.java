// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlAliasDeclaration;
import com.explyt.sql.psi.SqlSubquery;
import com.explyt.sql.psi.SqlSubqueryExpression;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class SqlSubqueryExpressionImpl extends SqlExpressionImpl implements SqlSubqueryExpression {

    public SqlSubqueryExpressionImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitSubqueryExpression(this);
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
    @NotNull
    public SqlSubquery getSubquery() {
        return findNotNullChildByClass(SqlSubquery.class);
    }

}
