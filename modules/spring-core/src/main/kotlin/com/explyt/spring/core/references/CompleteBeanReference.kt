/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.references

import com.explyt.spring.core.service.SpringBeanService
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType

class CompleteBeanReference(element: PsiElement, private val beanName: String, textRange: TextRange) :
    PsiReferenceBase<PsiElement>(element, textRange), PsiReference , HighlightedReference {

    override fun resolve(): PsiElement? {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return null
        val psiType = getPsiType() ?: return null

        val foundBeanDeclarations = SpringBeanService.getInstance(module.project).getBeanCandidates(module, psiType, beanName).asSequence()
            .map { it.psiMember }
            .toList()
        val resolveResults = foundBeanDeclarations.map { PsiElementResolveResult(it) }.toTypedArray()
        return if (resolveResults.size == 1) resolveResults[0].element else null
    }

    override fun getVariants(): Array<Any> {
        val variants = HashSet<Any>()

        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return emptyArray()
        val psiType = getPsiType() ?: return emptyArray()
        val beanCandidates = SpringBeanService.getInstance(module.project).getBeanCandidates(module, psiType, beanName)

        beanCandidates.mapTo(variants) {
            LookupElementBuilder.create(it.name)
                .withIcon(AllIcons.Nodes.Class)
                .withTailText(" (${it.psiClass.containingFile?.name})")
                .withTypeText(it.psiClass.name)
        }
        return variants.toTypedArray()
    }

    private fun getPsiType(): PsiType? {
        return when (val psiElement = element.parentOfType<PsiAnnotation>()?.parent?.parent) {
            is PsiField -> psiElement.type
            is PsiParameter -> psiElement.type
            else -> null
        }
    }
}
