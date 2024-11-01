// This is a generated file. Not intended for manual editing.
package com.explyt.spring.core.language.profiles.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;

import static com.explyt.spring.core.language.profiles.psi.ProfilesTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.explyt.spring.core.language.profiles.psi.*;

public class ProfilesOrExpressionImpl extends ASTWrapperPsiElement implements ProfilesOrExpression {

  public ProfilesOrExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ProfilesVisitor visitor) {
    visitor.visitOrExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ProfilesVisitor) accept((ProfilesVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<ProfilesAndExpression> getAndExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ProfilesAndExpression.class);
  }

  @Override
  @NotNull
  public List<ProfilesNotExpression> getNotExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ProfilesNotExpression.class);
  }

  @Override
  @NotNull
  public List<ProfilesOrExpression> getOrExpressionList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ProfilesOrExpression.class);
  }

  @Override
  @NotNull
  public List<ProfilesProfile> getProfileList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, ProfilesProfile.class);
  }

}
