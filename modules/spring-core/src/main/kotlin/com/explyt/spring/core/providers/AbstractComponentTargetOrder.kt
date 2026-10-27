/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClassOrObject

internal fun Collection<PsiElement>.sortedInAbstractComponentTargetOrder(): List<PsiElement> =
    map { TargetSortKey(it) }
        .sortedWith(TargetSortKey.order)
        .map { it.target }

private class TargetSortKey(val target: PsiElement) {
    private val navigationElement = target.navigationElement
    val presentableText: String = SymbolPresentationUtil.getSymbolPresentableText(target)
    val containingClassName: String =
        PsiTreeUtil.getNonStrictParentOfType(navigationElement, KtClassOrObject::class.java)?.fqName?.asString()
            ?: PsiTreeUtil.getNonStrictParentOfType(navigationElement, PsiClass::class.java)?.qualifiedName
            ?: ""
    val filePath: String = navigationElement.containingFile?.virtualFile?.path.orEmpty()
    val offset: Int = navigationElement.textOffset

    companion object {
        val order: Comparator<TargetSortKey> = compareBy<TargetSortKey> { it.presentableText }
            .thenBy { it.containingClassName }
            .thenBy { it.filePath }
            .thenBy { it.offset }
    }
}
