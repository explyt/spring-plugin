package com.esprito.spring.core.language.profiles.psi.impl

import com.esprito.spring.core.language.profiles.psi.ProfilesNamedElement
import com.intellij.extapi.psi.ASTWrapperPsiElement
import com.intellij.lang.ASTNode

abstract class ProfilesNamedElementImpl(node: ASTNode) :
    ASTWrapperPsiElement(node),
    ProfilesNamedElement