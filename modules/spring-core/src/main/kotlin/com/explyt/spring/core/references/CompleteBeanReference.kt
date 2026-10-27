/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references

import com.explyt.spring.core.service.SpringBeanService
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.TextRange
import com.intellij.psi.*
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UVariable
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

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

    private fun getPsiType(): PsiType? =
        (element.toUElement()?.getParentOfType<UAnnotation>()?.uastParent as? UVariable)?.type
}
