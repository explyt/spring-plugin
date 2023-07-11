// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.esprito.jpa.ql.psi.JpqlTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.esprito.jpa.ql.psi.*;

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
