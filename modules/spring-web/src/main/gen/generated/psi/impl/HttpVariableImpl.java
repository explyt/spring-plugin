// This is a generated file. Not intended for manual editing.
package generated.psi.impl;

import com.explyt.spring.web.language.http.psi.HttpVariable;
import com.explyt.spring.web.language.http.psi.HttpVisitor;
import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElementVisitor;
import org.jetbrains.annotations.NotNull;

public class HttpVariableImpl extends ASTWrapperPsiElement implements HttpVariable {

  public HttpVariableImpl(@NotNull ASTNode node) {
    super(node);
  }

  public void accept(@NotNull HttpVisitor visitor) {
    visitor.visitVariable(this);
  }

  @Override
  public void accept(@NotNull PsiElementVisitor visitor) {
    if (visitor instanceof HttpVisitor) accept((HttpVisitor)visitor);
    else super.accept(visitor);
  }

}
