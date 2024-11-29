// This is a generated file. Not intended for manual editing.
package com.explyt.jpa.ql.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

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
