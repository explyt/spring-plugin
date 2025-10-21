// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface SqlSubqueryFromClause extends SqlAliasHost {

    @NotNull
    List<SqlAliasDeclaration> getAliasDeclarationList();

    @NotNull
    List<SqlCollectionMemberDeclaration> getCollectionMemberDeclarationList();

    @NotNull
    List<SqlDerivedCollectionMemberDeclaration> getDerivedCollectionMemberDeclarationList();

    @NotNull
    List<SqlExpression> getExpressionList();

    @NotNull
    List<SqlIdentificationVariableDeclaration> getIdentificationVariableDeclarationList();

}
