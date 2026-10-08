/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration

internal object SourceNames {

    fun shortNameOf(method: PsiMethod): String {
        val declaration = kotlinOwnerOf(method)
        val owner = if (declaration is KtObjectDeclaration && declaration.isCompanion()) {
            sourceOwnerOf(method)?.name ?: method.containingClass?.name ?: "?"
        } else {
            method.containingClass?.name ?: "?"
        }
        return "$owner.${sourceNameOf(method)}"
    }

    fun qualifiedNameOf(method: PsiMethod, namedAfter: PsiClass? = null): String? {
        val owner = namedAfter?.qualifiedName
            ?: sourceOwnerOf(method)?.fqName?.asString()
            ?: method.containingClass?.qualifiedName
            ?: return null
        return "$owner.${sourceNameOf(method)}"
    }

    private fun sourceOwnerOf(method: PsiMethod): KtClassOrObject? {
        val declaration = kotlinOwnerOf(method) ?: return null
        if (declaration !is KtObjectDeclaration || !declaration.isCompanion()) return declaration
        return PsiTreeUtil.getParentOfType(declaration, KtClassOrObject::class.java) ?: declaration
    }

    private fun kotlinOwnerOf(method: PsiMethod): KtClassOrObject? =
        (method as? KtLightMethod)?.kotlinOrigin
            ?.let { PsiTreeUtil.getParentOfType(it, KtClassOrObject::class.java) }
            ?: (method.containingClass?.navigationElement as? KtClassOrObject)

    fun sourceNameOf(method: PsiMethod): String =
        ((method as? KtLightMethod)?.kotlinOrigin as? KtNamedFunction)?.name ?: method.name
}
