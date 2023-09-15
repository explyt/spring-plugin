package com.esprito.spring.core.service

import com.intellij.psi.PsiClass
import java.util.*

data class PsiBean(
    val name: String,
    val psiClass: PsiClass
) {
    constructor(psiClass: PsiClass) : this(
        psiClass.name?.replaceFirstChar { it.lowercase(Locale.getDefault()) } ?: ""
        , psiClass
    )
}