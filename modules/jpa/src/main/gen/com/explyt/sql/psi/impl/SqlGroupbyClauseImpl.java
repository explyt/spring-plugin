// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlGroupbyClause;
import com.explyt.sql.psi.SqlGroupbyItem;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlGroupbyClauseImpl extends ASTWrapperPsiElement implements SqlGroupbyClause {

    public SqlGroupbyClauseImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitGroupbyClause(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlGroupbyItem> getGroupbyItemList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlGroupbyItem.class);
    }

}
