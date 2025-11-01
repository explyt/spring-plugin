// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi.impl;

import com.explyt.sql.psi.SqlIdentificationVariableDeclaration;
import com.explyt.sql.psi.SqlJoinExpression;
import com.explyt.sql.psi.SqlTableNameRef;
import com.explyt.sql.psi.SqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class SqlIdentificationVariableDeclarationImpl extends ASTWrapperPsiElement implements SqlIdentificationVariableDeclaration {

    public SqlIdentificationVariableDeclarationImpl(@NotNull ASTNode node) {
        super(node);
    }

    public void accept(@NotNull SqlVisitor visitor) {
        visitor.visitIdentificationVariableDeclaration(this);
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof SqlVisitor) accept((SqlVisitor) visitor);
        else super.accept(visitor);
    }

    @Override
    @NotNull
    public List<SqlJoinExpression> getJoinExpressionList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, SqlJoinExpression.class);
    }

    @Override
    @NotNull
    public SqlTableNameRef getTableNameRef() {
        return findNotNullChildByClass(SqlTableNameRef.class);
    }

}
