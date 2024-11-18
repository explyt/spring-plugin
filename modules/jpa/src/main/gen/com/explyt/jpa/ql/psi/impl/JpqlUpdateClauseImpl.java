// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi.impl;

import com.explyt.jpa.ql.psi.JpqlEntityAccess;
import com.explyt.jpa.ql.psi.JpqlUpdateClause;
import com.explyt.jpa.ql.psi.JpqlUpdateItem;
import com.explyt.jpa.ql.psi.JpqlVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class JpqlUpdateClauseImpl extends ASTWrapperPsiElement implements JpqlUpdateClause {

  public JpqlUpdateClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitUpdateClause(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof JpqlVisitor) accept((JpqlVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public JpqlEntityAccess getEntityAccess() {
    return findNotNullChildByClass(JpqlEntityAccess.class);
  }

  @Override
  @NotNull
  public List<JpqlUpdateItem> getUpdateItemList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlUpdateItem.class);
  }

}
