// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import static com.explyt.spring.web.language.http.psi.HttpTypes.*;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.explyt.spring.web.language.http.psi.*;

public class HttpRequestLineImpl extends ASTWrapperPsiElement implements HttpRequestLine {

  public HttpRequestLineImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HttpVisitor visitor) {
    visitor.visitRequestLine(this);
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
  public HttpRequestTarget getRequestTarget() {
    return findNotNullChildByClass(HttpRequestTarget.class);
  }

  @Override
  @Nullable
  public PsiElement getHttpVersion() {
    return findChildByType(HTTP_VERSION);
  }

}
