// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import com.explyt.spring.web.language.http.psi.HttpMethod;
import com.explyt.spring.web.language.http.psi.HttpVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class HttpMethodImpl extends ASTWrapperPsiElement implements HttpMethod {

  public HttpMethodImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HttpVisitor visitor) {
    visitor.visitMethod(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HttpVisitor) accept((HttpVisitor)visitor);
    else super.accept(visitor);
  }

}
