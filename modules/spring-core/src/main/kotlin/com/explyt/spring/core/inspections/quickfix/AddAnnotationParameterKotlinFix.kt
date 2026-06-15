/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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