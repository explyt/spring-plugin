// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlSubqueryFromClauseImpl extends SqlAliasHostImpl implements SqlSubqueryFromClause {

    public SqlSubqueryFromClauseImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitSubqueryFromClause(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlAliasDeclaration> getAliasDeclarationList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlAliasDeclaration.class);
    }

    @Override
    @NotNull
    public List<SqlCollectionMemberDeclaration> getCollectionMemberDeclarationList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlCollectionMemberDeclaration.class);
    }

    @Override
    @NotNull
    public List<SqlDerivedCollectionMemberDeclaration> getDerivedCollectionMemberDeclarationList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlDerivedCollectionMemberDeclaration.class);
    }

    @Override
    @NotNull
    public List<SqlExpression> getExpressionList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlExpression.class);
    }

    @Override
    @NotNull
    public List<SqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlIdentificationVariableDeclaration.class);
    }

}
