// This is a generated file. Not intended for manual editing.
package com.esprito.jpql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlSubselectIdentificationVariableDeclaration extends PsiElement {

  @Nullable
  JpqlDerivedCollectionMemberDeclaration getDerivedCollectionMemberDeclaration();

  @NotNull
  List<JpqlExpression> getExpressionList();

  @Nullable
  JpqlIdentificationVariableDeclaration getIdentificationVariableDeclaration();

  @Nullable
  JpqlIdentifier getIdentifier();

}
