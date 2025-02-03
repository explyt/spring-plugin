// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import com.explyt.spring.web.language.http.psi.HttpMethod;
import com.explyt.spring.web.language.http.psi.HttpRequest;
import com.explyt.spring.web.language.http.psi.HttpUrl;
import com.explyt.spring.web.language.http.psi.HttpVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class HttpRequestImpl extends ASTWrapperPsiElement implements HttpRequest {

  public HttpRequestImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HttpVisitor visitor) {
    visitor.visitRequest(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HttpVisitor) accept((HttpVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @Nullable
  public HttpMethod getMethod() {
    return findChildByClass(HttpMethod.class);
  }

  @Override
  @NotNull
  public HttpUrl getUrl() {
    return findNotNullChildByClass(HttpUrl.class);
  }

}
