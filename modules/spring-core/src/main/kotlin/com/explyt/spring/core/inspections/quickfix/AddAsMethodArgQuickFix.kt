/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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