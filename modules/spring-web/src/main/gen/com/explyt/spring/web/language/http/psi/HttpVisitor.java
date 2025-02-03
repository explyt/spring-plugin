// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class HttpVisitor extends PsiElementVisitor {

  public void visitMethod(@NotNull HttpMethod o) {
    visitPsiElement(o);
  }

  public void visitRequest(@NotNull HttpRequest o) {
    visitPsiElement(o);
  }

  public void visitRequestBlock(@NotNull HttpRequestBlock o) {
    visitPsiElement(o);
  }

  public void visitRequests(@NotNull HttpRequests o) {
    visitPsiElement(o);
  }

  public void visitUrl(@NotNull HttpUrl o) {
    visitPsiElement(o);
  }

  public void visitVariable(@NotNull HttpVariable o) {
    visitPsiElement(o);
  }

  public void visitPsiElement(@NotNull PsiElement o) {
    visitElement(o);
  }

}
