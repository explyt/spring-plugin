package com.esprito.spring.core.service

import com.esprito.spring.core.util.SpringCoreUtil.getBeanName
import com.intellij.psi.PsiClass

data class PsiBean(
    val name: String,
    val psiClass: PsiClass
) {
    constructor(psiClass: PsiClass) : this(
        psiClass.getBeanName() ?: "",
        psiClass
    )

}