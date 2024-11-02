// This is a generated file. Not intended for manual editing.
package com.explyt.spring.core.language.profiles.psi.impl;

import com.explyt.spring.core.language.profiles.psi.ProfilesProfile;
import com.explyt.spring.core.language.profiles.psi.ProfilesVisitor;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

import static com.explyt.spring.core.language.profiles.psi.ProfilesTypes.VALUE;

public class ProfilesProfileImpl extends ProfilesNamedElementImpl implements ProfilesProfile {

  public ProfilesProfileImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull ProfilesVisitor visitor) {
    visitor.visitProfile(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof ProfilesVisitor) accept((ProfilesVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public PsiElement getValue() {
    return findNotNullChildByType(VALUE);
  }

  @Override
  @NotNull
  public String getName() {
    return ProfilesPsiImplUtil.getName(this);
  }

  @Override
  @NotNull
  public PsiElement setName(@NotNull String newName) {
    return ProfilesPsiImplUtil.setName(this, newName);
  }

  @Override
  @NotNull
  public PsiElement getNameIdentifier() {
    return ProfilesPsiImplUtil.getNameIdentifier(this);
  }

}
