// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlIdentificationVariableDeclaration extends PsiElement {

  @NotNull
  JpqlEntityAccess getEntityAccess();

  @NotNull
  List<JpqlFetchJoin> getFetchJoinList();

  @NotNull
  List<JpqlJoinExpression> getJoinExpressionList();

}
