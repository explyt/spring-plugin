package com.esprito.spring.core.inspections.quickfix

import com.esprito.spring.core.SpringCoreBundle
import com.intellij.codeInspection.LocalQuickFixAndIntentionActionOnPsiElement
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.*
import com.intellij.psi.util.parentOfType


/**
 * This class provides a solution to inspection
 */
class AddAsMethodArgQuickFix(identifier: PsiIdentifier) :
    LocalQuickFixAndIntentionActionOnPsiElement(identifier) {

    override fun getFamilyName(): String =
        SpringCoreBundle.message("esprito.spring.inspection.configuration.proxy.quickfix")

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

        val elementFactory = JavaPsiFacade.getInstance(project).elementFactory
        val parameter = elementFactory.createParameter(methodName, type)
        val usageOfParameter = elementFactory.createExpressionFromText(methodName, parameter)

        surroundingMethod.parameterList.add(parameter)
        methodCall.replace(usageOfParameter)
    }

}