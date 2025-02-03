// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import com.explyt.spring.web.language.http.psi.HttpRequest;
import com.explyt.spring.web.language.http.psi.HttpRequestBlock;
import com.explyt.spring.web.language.http.psi.HttpVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class HttpRequestBlockImpl extends ASTWrapperPsiElement implements HttpRequestBlock {

  public HttpRequestBlockImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HttpVisitor visitor) {
    visitor.visitRequestBlock(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HttpVisitor) accept((HttpVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public HttpRequest getRequest() {
    return findNotNullChildByClass(HttpRequest.class);
  }

}
