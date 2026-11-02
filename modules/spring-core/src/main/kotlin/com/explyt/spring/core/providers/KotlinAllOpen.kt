/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.compiler.configuration.KotlinCommonCompilerArgumentsHolder
import org.jetbrains.kotlin.idea.facet.getInstance
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtClassOrObject

internal object KotlinAllOpen {

    private const val OPTION_PREFIX = "plugin:org.jetbrains.kotlin.allopen:"
    private const val ANNOTATION = "annotation="
    private const val PRESET = "preset="

    private val presets = mapOf(
        "spring" to listOf(
            SpringCoreClasses.COMPONENT,
            SpringCoreClasses.TRANSACTIONAL,
            SpringCoreClasses.ASYNC,
            SpringCoreClasses.CACHEABLE,
            SpringCoreClasses.SPRING_BOOT_TEST,
            SpringCoreClasses.VALIDATED,
        )
    )

    fun opens(psiClass: PsiClass): Boolean {
        if (psiClass.language != KotlinLanguage.INSTANCE) return false
        val declaration = psiClass.navigationElement as? KtClassOrObject ?: return false
        if (declaration.hasModifier(KtTokens.FINAL_KEYWORD)) return false
        val module = ModuleUtilCore.findModuleForPsiElement(declaration) ?: return false
        val annotations = annotationsOpenedIn(KotlinCommonCompilerArgumentsHolder.getInstance(module).pluginOptions)
        if (annotations.isEmpty()) return false
        return !InheritanceUtil.processSupers(psiClass, true) { !it.isMetaAnnotatedBy(annotations) }
    }

    fun keepsFinal(method: PsiMethod): Boolean {
        val declaration = method.navigationElement as? KtNamedFunction ?: return false
        return declaration.hasModifier(KtTokens.FINAL_KEYWORD) || declaration.hasModifier(KtTokens.PRIVATE_KEYWORD)
    }

    private fun annotationsOpenedIn(pluginOptions: Array<String>?): List<String> =
        pluginOptions.orEmpty()
            .filter { it.startsWith(OPTION_PREFIX) }
            .map { it.removePrefix(OPTION_PREFIX) }
            .flatMap { option ->
                when {
                    option.startsWith(ANNOTATION) -> listOf(option.removePrefix(ANNOTATION))
                    option.startsWith(PRESET) -> presets[option.removePrefix(PRESET)].orEmpty()
                    else -> emptyList()
                }
            }
}
