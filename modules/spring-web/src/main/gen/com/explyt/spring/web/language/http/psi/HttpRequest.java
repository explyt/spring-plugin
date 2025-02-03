// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface HttpRequest extends PsiElement {

  @Nullable
  HttpMethod getMethod();

  @NotNull
  HttpUrl getUrl();

}
