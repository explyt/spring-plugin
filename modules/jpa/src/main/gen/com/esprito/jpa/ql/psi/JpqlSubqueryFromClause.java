// This is a generated file. Not intended for manual editing.
package com.esprito.jpa.ql.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface JpqlSubqueryFromClause extends JpqlAliasHost {

  @NotNull
  List<JpqlAliasDeclaration> getAliasDeclarationList();

  @NotNull
  List<JpqlCollectionMemberDeclaration> getCollectionMemberDeclarationList();

  @NotNull
  List<JpqlDerivedCollectionMemberDeclaration> getDerivedCollectionMemberDeclarationList();

  @NotNull
  List<JpqlExpression> getExpressionList();

  @NotNull
  List<JpqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList();

}
