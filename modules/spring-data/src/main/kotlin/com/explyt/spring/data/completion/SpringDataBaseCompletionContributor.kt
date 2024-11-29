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

package com.explyt.spring.data.completion

import com.explyt.spring.data.util.SpringDataUtil
import com.intellij.codeInsight.completion.CompletionContributor
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.lang.java.JavaLanguage
import com.intellij.patterns.*
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiField
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.ProcessingContext
import org.jetbrains.uast.*


class SpringDataBaseCompletionContributor : CompletionContributor() {

    init {
        val javaFieldCapture = PsiJavaPatterns.psiElement(
            PsiIdentifier::class.java
        ).withParent(PsiField::class.java)
            .with(object : PatternCondition<PsiElement>(SpringDataBaseCompletionContributor::class.java.simpleName) {
                override fun accepts(psiElement: PsiElement, context: ProcessingContext): Boolean {
                    val psiClass = PsiTreeUtil.getParentOfType(psiElement, PsiClass::class.java)
                    return psiClass != null && SpringDataUtil.isRepository(psiClass)
                }
            })

        val uastMethodCapture: ElementPattern<PsiElement> = object : PsiElementPattern.Capture<PsiElement>(
            PsiElement::class.java
        ) {
            override fun accepts(elementObject: Any?, context: ProcessingContext): Boolean {
                val element = elementObject as? PsiElement ?: return false
                if (element.containingFile.language.`is`(JavaLanguage.INSTANCE)) return false
                val uIdentifier = element.toUElement(UIdentifier::class.java) ?: return false
                val intermediateParent: UMethod = uIdentifier.skipParentOfType(
                    true, UReferenceExpression::class.java, UTypeReferenceExpression::class.java
                ) as? UMethod ?: return false
                val returnTypeReference = intermediateParent.returnTypeReference
                if (returnTypeReference != null && isPsiAncestor(returnTypeReference, uIdentifier)) {
                    return false
                }
                val uClass = element.toUElement()?.getParentOfType(UClass::class.java) ?: return false
                return SpringDataUtil.isRepository(uClass.javaPsi)
            }
        }

        val completionPattern = StandardPatterns.or(javaFieldCapture, uastMethodCapture)
        extend(CompletionType.BASIC, completionPattern, SpringDataBaseCompletionProvider())
    }
}