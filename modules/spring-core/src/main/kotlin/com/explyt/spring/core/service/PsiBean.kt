package com.explyt.spring.core.service

import com.explyt.spring.core.util.SpringCoreUtil.getBeanName
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember

data class PsiBean(
    val name: String,
    val psiClass: PsiClass,
    val psiQualifier: PsiAnnotation? = null,
    val psiMember: PsiMember = psiClass,
    val isPrimary: Boolean = false,
) {

    constructor(psiClass: PsiClass) : this(
        name = psiClass.getBeanName() ?: "",
        psiClass = psiClass,
        psiMember = psiClass
    )

}