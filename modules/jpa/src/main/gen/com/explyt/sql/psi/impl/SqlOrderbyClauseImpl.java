// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlOrderbyClause;
import com.explyt.sql.psi.SqlOrderbyItem;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlOrderbyClauseImpl extends ASTWrapperPsiElement implements SqlOrderbyClause {

    public SqlOrderbyClauseImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitOrderbyClause(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlOrderbyItem> getOrderbyItemList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlOrderbyItem.class);
    }

}
