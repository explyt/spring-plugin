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

public class ProfilesNotExpressionImpl extends ASTWrapperPsiElement implements ProfilesNotExpression {

  public ProfilesNotExpressionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ProfilesVisitor visitor) {
    visitor.visitNotExpression(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ProfilesVisitor) accept((ProfilesVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public ProfilesAndExpression getAndExpression() {
    return findChildByClass(ProfilesAndExpression.class);
  }

  @Override
  @Nullable
  public ProfilesNotExpression getNotExpression() {
    return findChildByClass(ProfilesNotExpression.class);
  }

  @Override
  @Nullable
  public ProfilesOrExpression getOrExpression() {
    return findChildByClass(ProfilesOrExpression.class);
  }

  @Override
  @Nullable
  public ProfilesProfile getProfile() {
    return findChildByClass(ProfilesProfile.class);
  }

}
