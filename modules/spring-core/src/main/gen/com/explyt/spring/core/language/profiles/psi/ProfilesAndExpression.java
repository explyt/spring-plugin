// This is a generated file. Not intended for manual editing.
package com.explyt.spring.core.language.profiles.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ProfilesAndExpression extends PsiElement {

  @NotNull
  List<ProfilesAndExpression> getAndExpressionList();

  @NotNull
  List<ProfilesNotExpression> getNotExpressionList();

  @NotNull
  List<ProfilesOrExpression> getOrExpressionList();

  @NotNull
  List<ProfilesProfile> getProfileList();

}
