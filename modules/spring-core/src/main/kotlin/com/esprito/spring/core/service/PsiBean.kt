package com.esprito.spring.core.service

import com.intellij.psi.PsiClass
import java.util.*

data class PsiBean(
    val name: String,
    val psiClass: PsiClass
) {
    constructor(psiClass: PsiClass) : this(
        getBeanName(psiClass) ?: "",
        psiClass
    )

    companion object {
        fun getBeanName(psiClass: PsiClass): String? = getBeanName(psiClass.name)
        fun getBeanName(className: String?): String? =
            className?.replaceFirstChar { it.lowercase(Locale.getDefault()) }
    }

}