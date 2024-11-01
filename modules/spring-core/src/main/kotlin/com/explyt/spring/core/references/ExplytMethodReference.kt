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

import com.explyt.spring.core.util.SpringCoreUtil.resolveBeanPsiClass
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
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUastParentOfType

class ExplytMethodReference(element: PsiElement, private val methodName: String, rangeInElement: TextRange) :
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
        val uastParent = element
            .getUastParentOfType<UAnnotation>()
            ?.uastParent ?: return null

        return when (uastParent) {
            is UClass -> uastParent.javaPsi
            is UMethod -> uastParent.javaPsi.returnType?.resolveBeanPsiClass
            else -> null
        }
    }

}