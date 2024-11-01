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

package com.explyt.spring.core.inspections.quickfix

import com.explyt.spring.core.SpringCoreBundle.message
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.uast.UAnnotated
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class AddAnnotationParameterKotlinFix(
    psiElement: PsiElement,
    private val annotationFqName: String,
    private val argumentValue: String
) : LocalQuickFixAndIntentionActionOnPsiElement(psiElement) {
    override fun getText() = message(
        "explyt.spring.inspection.kotlin.internal.fix",
        "@" + ClassId.topLevel(FqName(annotationFqName)).shortClassName
    )

    override fun getFamilyName() = text

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val uAnnotated = startElement.toUElement()?.getParentOfType<UAnnotated>() ?: return
        val annotationEntry = uAnnotated.findAnnotation(annotationFqName)?.sourcePsi as? KtAnnotationEntry ?: return

        val psiFactory = KtPsiFactory(project)
        annotationEntry.valueArgumentList?.addArgument(psiFactory.createArgument("value = [\"$argumentValue\"]"))
            ?: annotationEntry
                .addAfter(psiFactory.createArgument("(\"$argumentValue\")"), annotationEntry.lastChild)
        ShortenReferencesFacility.getInstance().shorten(annotationEntry)
    }
}