// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlCollectionMemberDeclaration;
import com.explyt.sql.psi.SqlFromClause;
import com.explyt.sql.psi.SqlIdentificationVariableDeclaration;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlFromClauseImpl extends SqlAliasHostImpl implements SqlFromClause {

    public SqlFromClauseImpl(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitFromClause(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlCollectionMemberDeclaration> getCollectionMemberDeclarationList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlCollectionMemberDeclaration.class);
    }

    @Override
    @NotNull
    public List<SqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlIdentificationVariableDeclaration.class);
    }

}
