package com.esprito.jpa.ql.psi.impl

import com.esprito.jpa.ql.psi.JpqlNameIdentifierOwner
import com.intellij.lang.ASTNode

abstract class JpqlNameIdentifierOwnerImpl(astNode: ASTNode) : JpqlNamedElementImpl(astNode), JpqlNameIdentifierOwner {
}