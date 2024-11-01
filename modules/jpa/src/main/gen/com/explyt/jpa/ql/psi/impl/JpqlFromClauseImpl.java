// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlCollectionMemberDeclaration;
import com.explyt.jpa.ql.psi.JpqlFromClause;
import com.explyt.jpa.ql.psi.JpqlIdentificationVariableDeclaration;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlFromClauseImpl extends JpqlAliasHostImpl implements JpqlFromClause {

  public JpqlFromClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitFromClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<JpqlCollectionMemberDeclaration> getCollectionMemberDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlCollectionMemberDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlIdentificationVariableDeclaration.class);
  }

}
