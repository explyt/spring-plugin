// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.esprito.jpa.ql.psi.JpqlTypes.*;
import com.esprito.jpa.ql.psi.*;

public class JpqlSubqueryFromClauseImpl extends JpqlAliasHostImpl implements JpqlSubqueryFromClause {

  public JpqlSubqueryFromClauseImpl(@NotNull ASTNode node) {
    super(node);
  }

  @Override
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
  public List<JpqlAliasDeclaration> getAliasDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlAliasDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlCollectionMemberDeclaration> getCollectionMemberDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlCollectionMemberDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlDerivedCollectionMemberDeclaration> getDerivedCollectionMemberDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlDerivedCollectionMemberDeclaration.class);
  }

  @Override
  @NotNull
  public List<JpqlExpression> getExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlExpression.class);
  }

  @Override
  @NotNull
  public List<JpqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, JpqlIdentificationVariableDeclaration.class);
  }

}
