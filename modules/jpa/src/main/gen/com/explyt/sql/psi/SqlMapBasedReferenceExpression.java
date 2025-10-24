// This is a generated file. Not intended for manual editing.
package com.explyt.sql.psi;

import com.explyt.jpa.ql.psi.JpqlElementType;
import org.jetbrains.annotations.NotNull;

public interface SqlMapBasedReferenceExpression extends SqlReferenceExpression {

    @NotNull
    SqlReferenceExpression getReferenceExpression();

    @NotNull JpqlElementType getMapOperationType();

}
