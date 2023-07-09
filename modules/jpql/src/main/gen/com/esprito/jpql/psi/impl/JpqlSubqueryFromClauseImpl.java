// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.esprito.jpql.psi.JpqlTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.esprito.jpql.psi.*;

public class JpqlSubqueryFromClauseImpl extends ASTWrapperPsiElement implements JpqlSubqueryFromClause {

  public JpqlSubqueryFromClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull JpqlVisitor visitor) {
    visitor.visitSubqueryFromClause(this);
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
  public List<JpqlSubselectIdentificationVariableDeclaration> getSubselectIdentificationVariableDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlSubselectIdentificationVariableDeclaration.class);
  }

}
