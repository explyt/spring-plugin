/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.util

import com.explyt.base.BaseBundle
import com.intellij.codeInsight.intention.impl.invokeAsAction
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.idea.codeinsight.utils.findExistingEditor
import org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix
import org.jetbrains.kotlin.idea.quickfix.AddAnnotationFix.Kind
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.startOffset
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.toUElement


object QuickFixUtil {
    fun addClassAnnotationFix(uClass: UClass, annotationFqName: String): Array<LocalQuickFix> {
        if (uClass.lang === KotlinLanguage.INSTANCE) {
            return arrayOf(AddClassAnnotationKotlinFix(annotationFqName))
        } else {
            return arrayOf(com.intellij.codeInsight.intention.AddAnnotationFix(annotationFqName, uClass.javaPsi))
        }
    }
}

private class AddClassAnnotationKotlinFix(private val annotationFqName: String) : LocalQuickFix {
    override fun getName() = BaseBundle.message(
        "explyt.base.inspection.add.annotate.fix",
        "@" + ClassId.topLevel(FqName(annotationFqName)).shortClassName
    )

    override fun getFamilyName() = name
    override fun startInWriteAction() = false

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {

        val uAnnotation = descriptor.psiElement.toUElement() as? UAnnotation ?: return
        val ktElement = uAnnotation.getContainingUClass()?.sourcePsi as? KtElement ?: return
        val editor = ktElement.findExistingEditor() ?: return
        val ktFile = ktElement.containingFile as? KtFile ?: return

        ApplicationManager.getApplication().runWriteIntentReadAction(ThrowableComputable {
            AddAnnotationFix(
                ktElement, ClassId.topLevel(FqName(annotationFqName)), Kind.Self
            ).asIntention().invokeAsAction(editor, ktFile)
        })
    }
}

class AddAnnotationValueQuickFix(
    psiAnnotation: PsiElement, private val parameterName: String, private val parameterValue: String
) :
    LocalQuickFixAndIntentionActionOnPsiElement(psiAnnotation) {

    override fun getFamilyName(): String =
        BaseBundle.message("explyt.base.inspection.add.annotate.value.fix", parameterName)

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        val fakeAnnotationText = "@FakeAnnotation($parameterName = \"$parameterValue\")"
        if (startElement is KtAnnotationEntry) {
            val fakeAnnotation = KtPsiFactory(project).createAnnotationEntry(fakeAnnotationText)
            val valueArgument = fakeAnnotation.valueArguments.first() as KtValueArgument

            val valueArgumentList = startElement.valueArgumentList
            if (valueArgumentList != null) {
                valueArgumentList.addArgument(valueArgument)
            } else {
                startElement.add(fakeAnnotation.valueArgumentList!!)
            }
            navigateToValue(startElement.toUElement() as? UAnnotation, parameterName, editor)
            return
        }

        val fakeAnnotation = JavaPsiFacade.getElementFactory(project)
            .createAnnotationFromText(fakeAnnotationText, null)
        val psiAnnotation = startElement as? PsiAnnotation ?: return
        val psiNameValuePair = fakeAnnotation.parameterList.attributes[0]
        psiAnnotation.parameterList.add(psiNameValuePair)
        navigateToValue(psiAnnotation.toUElement() as? UAnnotation, parameterName, editor)
    }

    private fun navigateToValue(uAnnotation: UAnnotation?, parameterName: String, editor: Editor?) {
        if (!ApplicationManager.getApplication().isWriteAccessAllowed) return
        uAnnotation ?: return
        val expression = uAnnotation.attributeValues.find { it.name == parameterName }?.expression ?: return
        val sourcePsi = expression.sourcePsi ?: return

        editor?.caretModel?.moveToOffset(sourcePsi.startOffset + 1)
        editor?.selectionModel?.selectWordAtCaret(true)
    }

}