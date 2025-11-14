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

package com.explyt.spring.web.inspections.quickfix

import com.explyt.spring.core.statistic.StatisticActionId.PREVIEW_REQUEST_MAPPING_ADD_PATH_VARIABLE
import com.explyt.spring.core.statistic.StatisticActionId.QUICK_FIX_REQUEST_MAPPING_ADD_PATH_VARIABLE
import com.explyt.spring.core.statistic.StatisticUtil.registerActionUsage
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.intellij.codeInsight.intention.AddAnnotationModCommandAction
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.kotlin.idea.base.codeInsight.ShortenReferencesFacility
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtPsiFactory


/**
 * This class provides a solution to inspection
 */
class AddPathVariableQuickFix(psiMethod: PsiElement, private val parameterName: String) :
    LocalQuickFixAndIntentionActionOnPsiElement(psiMethod) {

    override fun getFamilyName(): String =
        SpringWebBundle.message("explyt.spring.web.inspection.pathVariable.omittedParameter.fix")

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (startElement is KtFunction) {
            val parameterText = "@${SpringWebClasses.PATH_VARIABLE} $parameterName: String"
            val parameter = KtPsiFactory(project).createParameter(parameterText)
            startElement.valueParameterList?.addParameter(parameter)
            ShortenReferencesFacility.getInstance().shorten(startElement)
            return
        }
        val psiMethod = startElement as? PsiMethod ?: return

        editor.registerActionUsage(
            QUICK_FIX_REQUEST_MAPPING_ADD_PATH_VARIABLE,
            PREVIEW_REQUEST_MAPPING_ADD_PATH_VARIABLE
        )

        val psiManager = PsiManager.getInstance(project)
        val elementFactory = JavaPsiFacade.getInstance(project).elementFactory

        val stringType = PsiType.getJavaLangString(psiManager, GlobalSearchScope.projectScope(project))
        val newArgument = elementFactory.createParameter(parameterName, stringType)
        psiMethod.parameterList.add(newArgument)

        editor?.let { PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(it.document) }

        val psiParameter = psiMethod.parameterList.parameters.last()
        val action = AddAnnotationModCommandAction(SpringWebClasses.PATH_VARIABLE, psiParameter)
        action.asIntention().invoke(project, editor, file)

    }

}