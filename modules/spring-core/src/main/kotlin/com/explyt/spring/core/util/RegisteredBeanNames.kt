/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.intellij.psi.PsiClass
import com.intellij.psi.util.ClassUtil

object RegisteredBeanNames {

    fun scanned(psiClass: PsiClass, explicitName: String?): String? = explicitName ?: DefaultBeanNames.of(psiClass)

    fun imported(psiClass: PsiClass, explicitName: String?): String? = explicitName ?: binaryNameOf(psiClass)

    fun enabledProperties(psiClass: PsiClass, prefix: String?): String? =
        binaryNameOf(psiClass)?.let { if (prefix.isNullOrBlank()) it else "$prefix-$it" }

    private fun binaryNameOf(psiClass: PsiClass): String? = ClassUtil.getJVMClassName(psiClass) ?: psiClass.qualifiedName
}
