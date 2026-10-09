/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections

import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.inspections.quickfix.MoveControllerBeanNameToRequestMappingQuickFix
import com.explyt.spring.web.util.WebApplicationStack
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.util.InheritanceUtil
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass

class ControllerBeanNameAsPathInspection : SpringWebBaseUastLocalInspectionTool() {

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val stereotype = aClass.uAnnotations.firstOrNull { it.qualifiedName in CONTROLLER_STEREOTYPES } ?: return null
        val beanName = stereotype.pathLikeBeanName() ?: return null
        if (aClass.javaPsi.isBeanNameUrlHandler()) return null

        val fix = MoveControllerBeanNameToRequestMappingQuickFix(
            beanName.value,
            addRequestMapping = !aClass.javaPsi.hasAnnotation(SpringWebClasses.REQUEST_MAPPING)
        )
        return arrayOf(
            manager.createProblemDescriptor(
                beanName.literal,
                beanName.message(),
                isOnTheFly,
                arrayOf(fix),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    private class PathLikeBeanName(val value: String, val literal: PsiElement) {

        fun message(): String = SpringWebBundle.message(messageKey(), value)

        private fun messageKey(): String =
            if (value.startsWith("/") && isServletStack()) {
                "explyt.spring.web.inspection.controller.beanNameAsPath.servlet"
            } else {
                "explyt.spring.web.inspection.controller.beanNameAsPath.prefixIgnored"
            }

        private fun isServletStack(): Boolean {
            val module = ModuleUtilCore.findModuleForPsiElement(literal) ?: return false
            return WebApplicationStack.of(module) == WebApplicationStack.SERVLET
        }
    }

    private fun UAnnotation.pathLikeBeanName(): PathLikeBeanName? {
        val expression = findDeclaredAttributeValue(VALUE_ATTRIBUTE) ?: return null
        val value = expression.evaluate() as? String ?: return null
        if ('/' !in value) return null
        val sourcePsi = expression.sourcePsi ?: return null
        val literal = PsiTreeUtil.getParentOfType(sourcePsi, KtStringTemplateExpression::class.java, false) ?: sourcePsi
        return PathLikeBeanName(value, literal)
    }

    private fun PsiClass.isBeanNameUrlHandler(): Boolean =
        BEAN_NAME_URL_HANDLER_TYPES.any { InheritanceUtil.isInheritor(this, it) }

    companion object {
        private const val VALUE_ATTRIBUTE = "value"
        private val CONTROLLER_STEREOTYPES = setOf(SpringWebClasses.CONTROLLER, SpringWebClasses.REST_CONTROLLER)
        private val BEAN_NAME_URL_HANDLER_TYPES = listOf(
            SpringWebClasses.HTTP_REQUEST_HANDLER,
            SpringWebClasses.SERVLET_MVC_CONTROLLER
        )
    }
}
