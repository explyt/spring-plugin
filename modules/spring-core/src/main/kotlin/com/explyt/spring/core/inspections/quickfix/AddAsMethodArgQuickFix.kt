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

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.statistic.StatisticActionId.PREVIEW_PROXY_BEAN_METHODS
import com.explyt.spring.core.statistic.StatisticActionId.QUICK_FIX_PROXY_BEAN_METHODS
import com.explyt.spring.core.statistic.StatisticUtil.registerActionUsage
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType


class AddAsMethodArgQuickFix(identifier: PsiIdentifier) :
    LocalQuickFixAndIntentionActionOnPsiElement(identifier) {

    override fun getFamilyName(): String =
        SpringCoreBundle.message("explyt.spring.inspection.configuration.proxy.quickfix")

    override fun getText(): String = familyName

    override fun invoke(
        project: Project,
        file: PsiFile,
        editor: Editor?,
        startElement: PsiElement,
        endElement: PsiElement
    ) {
        if (startElement !is PsiIdentifier) return

        val methodName = startElement.text ?: return
        val methodCall = startElement.parentOfType<PsiMethodCallExpression>() ?: return
        val type = methodCall.resolveMethod()?.returnType ?: return
        val surroundingMethod = startElement.parentOfType<PsiMethod>() ?: return

        editor.registerActionUsage(
            QUICK_FIX_PROXY_BEAN_METHODS,
            PREVIEW_PROXY_BEAN_METHODS
        )

        val elementFactory = JavaPsiFacade.getInstance(project).elementFactory
        val parameter = elementFactory.createParameter(methodName, type)
        val usageOfParameter = elementFactory.createExpressionFromText(methodName, parameter)

        surroundingMethod.parameterList.add(parameter)
        methodCall.replace(usageOfParameter)
    }

}