// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlIdentifier;
import com.explyt.sql.psi.SqlInsertFields;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlInsertFieldsImpl extends ASTWrapperPsiElement implements SqlInsertFields {

    public SqlInsertFieldsImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitInsertFields(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlIdentifier> getIdentifierList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlIdentifier.class);
    }

}
