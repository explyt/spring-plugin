// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HttpRequestLine extends PsiElement {

  @Nullable
  HttpMethod getMethod();

  @NotNull
  HttpRequestTarget getRequestTarget();

  @Nullable
  PsiElement getHttpVersion();

}
