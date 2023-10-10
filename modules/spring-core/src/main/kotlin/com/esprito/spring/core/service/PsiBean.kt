package com.esprito.spring.core.service

import com.esprito.spring.core.util.SpringCoreUtil.getBeanName
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiModifierListOwner

data class PsiBean(
    val name: String,
    val psiClass: PsiClass,
    val psiQualifier: PsiAnnotation? = null,
    val psiMember: PsiModifierListOwner = psiClass
) {

    constructor(psiClass: PsiClass) : this(
        name = psiClass.getBeanName() ?: "",
        psiClass = psiClass,
        psiMember = psiClass
    )

}