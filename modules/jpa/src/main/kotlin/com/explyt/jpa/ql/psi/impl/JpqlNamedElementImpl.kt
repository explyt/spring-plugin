package com.explyt.jpa.ql.psi.impl

import com.explyt.jpa.ql.psi.JpqlNamedElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class JpqlNamedElementImpl(astNode: ASTNode) : ASTWrapperPsiElement(astNode), JpqlNamedElement