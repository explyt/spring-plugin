// This is a generated file. Not intended for manual editing.
package com.explyt.spring.core.language.profiles.psi;

import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiElement;

public class ProfilesVisitor extends PsiElementVisitor {

  public void visitAndExpression(@NotNull ProfilesAndExpression o) {
    visitPsiElement(o);
  }

  public void visitNotExpression(@NotNull ProfilesNotExpression o) {
    visitPsiElement(o);
  }

  public void visitOrExpression(@NotNull ProfilesOrExpression o) {
    visitPsiElement(o);
  }

  public void visitProfile(@NotNull ProfilesProfile o) {
    visitNamedElement(o);
  }

  public void visitNamedElement(@NotNull ProfilesNamedElement o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
