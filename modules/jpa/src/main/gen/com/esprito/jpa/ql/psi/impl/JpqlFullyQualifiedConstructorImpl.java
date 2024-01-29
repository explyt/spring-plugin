// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import com.esprito.jpa.ql.psi.JpqlFullyQualifiedConstructor;
import com.esprito.jpa.ql.psi.JpqlIdentifier;
import com.esprito.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlFullyQualifiedConstructorImpl extends JpqlReferenceExpressionImpl implements JpqlFullyQualifiedConstructor {
    
    public JpqlFullyQualifiedConstructorImpl(@NotNull ASTNode node) {
        super(node);
    }
    
    @Override
    public void accept(@NotNull JpqlVisitor visitor) {
        visitor.visitFullyQualifiedConstructor(this);
    }
    
    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JpqlVisitor) accept((JpqlVisitor) visitor);
        else super.accept(visitor);
    }
    
    @Override
    @NotNull
    public List<JpqlIdentifier> getIdentifierList() {
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlIdentifier.class);
    }
    
}
