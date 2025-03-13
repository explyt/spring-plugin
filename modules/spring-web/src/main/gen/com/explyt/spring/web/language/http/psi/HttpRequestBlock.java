// This is a generated file. Not intended for manual editing.
package com.explyt.spring.web.language.http.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface HttpRequestBlock extends PsiElement {

  @NotNull
  List<HttpComment> getCommentList();

  @NotNull
  HttpRequest getRequest();

  @Nullable
  HttpRequestDefiner getRequestDefiner();

}
