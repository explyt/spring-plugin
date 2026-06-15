/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.references

import com.explyt.spring.core.service.AliasUtils
import com.explyt.util.ExplytPsiUtil
import com.intellij.codeInsight.highlighting.HighlightedReference
import com.intellij.codeInsight.lookup.LookupElementBuilder
import com.intellij.icons.AllIcons
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class ExplytAliasMethodReference(element: PsiElement, private val methodName: String, rangeInElement: TextRange) :
    PsiReferenceBase<PsiElement>(element, rangeInElement), PsiReference, HighlightedReference {

    override fun resolve(): PsiElement? {
        val aClass = getRelatedClass() ?: return null

        return aClass.allMethods.asSequence()
            .filter { ExplytPsiUtil.fitsForReference(it) }
            .filter { it.name == methodName }
            .firstOrNull()
    }

    override fun getVariants(): Array<Any> {
        val aClass = getRelatedClass() ?: return emptyArray()

        return aClass.allMethods.asSequence()
            .filter { ExplytPsiUtil.fitsForReference(it) }
            .map { method ->
                LookupElementBuilder.create(method.name)
                    .withIcon(AllIcons.Nodes.MethodReference)
                    .withTypeText(method.containingFile?.name)
            }.toList().toTypedArray()
    }

    private fun getRelatedClass(): PsiClass? {
        val aliasAnnotation = element.toUElement()
            ?.getParentOfType<UAnnotation>()
            ?.javaPsi ?: return null

        return AliasUtils.getAliasedClass(aliasAnnotation)
    }

}