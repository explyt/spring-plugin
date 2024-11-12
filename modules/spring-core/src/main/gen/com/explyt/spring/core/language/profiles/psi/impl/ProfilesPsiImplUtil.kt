package com.explyt.spring.core.language.profiles.psi.impl

import com.explyt.spring.core.language.profiles.psi.ProfilesElementFactory
import com.explyt.spring.core.language.profiles.psi.ProfilesProfile
import com.explyt.spring.core.language.profiles.psi.ProfilesTypes
import com.intellij.psi.PsiElement

object ProfilesPsiImplUtil {

    @JvmStatic
    fun getNameIdentifier(element: ProfilesProfile): PsiElement {
        return element.value
    }

    @JvmStatic
    fun getName(element: ProfilesProfile): String {
        return element.value.text
    }

    @JvmStatic
    fun setName(element: ProfilesProfile, newName: String): PsiElement {
        val oldValue = element.node.findChildByType(ProfilesTypes.VALUE) ?: return element

        val newValue = ProfilesElementFactory.createValue(element.project, newName)
        element.node.replaceChild(oldValue, newValue.node)

        return element
    }

}