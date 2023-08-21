package com.esprito.jpa.ql.psi.impl

import com.esprito.jpa.ql.psi.JpqlNamedElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class JpqlNamedElementImpl(astNode: ASTNode) : ASTWrapperPsiElement(astNode), JpqlNamedElement