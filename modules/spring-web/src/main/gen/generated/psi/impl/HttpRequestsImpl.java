// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import com.explyt.spring.web.language.http.psi.HttpRequestBlock;
import com.explyt.spring.web.language.http.psi.HttpRequests;
import com.explyt.spring.web.language.http.psi.HttpVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public class HttpRequestsImpl extends ASTWrapperPsiElement implements HttpRequests {

  public HttpRequestsImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HttpVisitor visitor) {
    visitor.visitRequests(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HttpVisitor) accept((HttpVisitor)visitor);
    else super.accept(visitor);
  }

  @Override
  @NotNull
  public List<HttpRequestBlock> getRequestBlockList() {
    return PsiTreeUtil.getChildrenOfTypeAsList(this, HttpRequestBlock.class);
  }

}
