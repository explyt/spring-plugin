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

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseLocalInspectionTool
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringProperties
import com.explyt.spring.core.inspections.utils.ResourceFileInspectionUtil
import com.explyt.util.ExplytPsiUtil.isString
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.lang.properties.PropertiesFileType
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.util.InheritanceUtil
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.uast.*
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class PsiFileReferenceInspection : SpringBaseLocalInspectionTool() {
    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(PsiFileReferenceVisitor(holder, isOnTheFly), true)
    }
}

private class PsiFileReferenceVisitor(
    private val holder: ProblemsHolder, private val isOnTheFly: Boolean
) : AbstractUastNonRecursiveVisitor() {

    override fun visitAnnotation(node: UAnnotation): Boolean {
        checkValueAnnotation(node)
        return true
    }

    override fun visitCallExpression(node: UCallExpression): Boolean {
        checkCallExpression(node)
        checkConstructorCallExpression(node)
        return true
    }

    override fun visitQualifiedReferenceExpression(node: UQualifiedReferenceExpression): Boolean {
        if (node.lang == KotlinLanguage.INSTANCE) return true
        val uCallExpression = (node.selector as? UCallExpression) ?: return true
        checkCallExpression(uCallExpression)
        checkConstructorCallExpression(uCallExpression)
        return true
    }

    private fun checkValueAnnotation(node: UAnnotation) {
        if (node.qualifiedName != SpringCoreClasses.VALUE) return
        val psiElement = node.findAttributeValue("value")?.sourcePsi ?: return
        val valueText = ElementManipulators.getValueText(psiElement)
        val problems = ResourceFileInspectionUtil.getPathProblems(
            PropertiesFileType.INSTANCE,
            valueText,
            psiElement,
            holder.manager,
            isOnTheFly
        )
        problems.forEach { holder.registerProblem(it) }
    }

    private fun checkCallExpression(node: UCallExpression) {
        if (node.kind != UastCallKind.METHOD_CALL) return
        val methodName = node.methodName
        val psiMethod = node.resolve() ?: return
        val targetClass = psiMethod.containingClass ?: return
        checkResourceLoaderGetResource(node, targetClass, methodName)
        checkResourceClassGetResource(methodName, node, targetClass)
        checkResourceUtil(targetClass, node)

    }

    private fun checkConstructorCallExpression(node: UCallExpression) {
        if (node.kind != UastCallKind.CONSTRUCTOR_CALL) return
        val psiMethod = node.resolve() ?: return
        val targetClass = psiMethod.containingClass ?: return
        checkResourceLoaderGetResource(node, targetClass)
    }

    private fun checkResourceLoaderGetResource(
        node: UCallExpression,
        targetClass: PsiClass,
        methodName: String? = null
    ) {
        if (!methodName.isNullOrBlank() && methodName != SpringProperties.GET_RESOURCE) return
        if (node.valueArgumentCount != 1) return

        val uArgumentExpression = node.getArgumentForParameter(0) ?: return
        val psiElement = uArgumentExpression.sourcePsi ?: return
        val valueText = getExpression(uArgumentExpression) ?: return
        if (InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.RESOURCE_LOADER)
            || InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.FILE_RESOURCE_RESOURCE)
        ) {
            val problems = ResourceFileInspectionUtil.getPathProblems(
                PropertiesFileType.INSTANCE,
                valueText,
                psiElement,
                holder.manager,
                isOnTheFly
            )
            problems.forEach { holder.registerProblem(it) }
        }
    }

    private fun checkResourceClassGetResource(
        methodName: String?,
        node: UCallExpression,
        targetClass: PsiClass
    ) {
        if (methodName != SpringProperties.GET_RESOURCE || node.valueArgumentCount != 1) return
        val uArgumentExpression = node.getArgumentForParameter(0) ?: return
        val psiElement = uArgumentExpression.sourcePsi ?: return
        val valueText = getExpression(uArgumentExpression) ?: return
        if (InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.RESOURCE_CLASS)
            || InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.RESOURCE_CLASS_LOADER)
        ) {
            val problems = ResourceFileInspectionUtil.getPathClassResourceProblems(
                PropertiesFileType.INSTANCE,
                valueText,
                psiElement,
                holder.manager,
                isOnTheFly
            )
            problems.forEach { holder.registerProblem(it) }
        }
    }

    private fun checkResourceUtil(targetClass: PsiClass, node: UCallExpression) {
        if (!InheritanceUtil.isInheritor(targetClass, SpringCoreClasses.RESOURCE_UTILS)
            || node.valueArgumentCount != 1
        ) return

        val uArgumentExpression = node.getArgumentForParameter(0) ?: return
        if (uArgumentExpression.getExpressionType()?.isString == false) return
        if (!uArgumentExpression.isInjectionHost()) return

        val psiElement = uArgumentExpression.sourcePsi ?: return
        val valueText = ElementManipulators.getValueText(psiElement)
        val problems = ResourceFileInspectionUtil.getPathProblems(
            PropertiesFileType.INSTANCE,
            valueText,
            psiElement,
            holder.manager,
            isOnTheFly
        )
        problems.forEach { holder.registerProblem(it) }
    }

    private fun getExpression(uArgumentExpression: UExpression): String? {
        val psiElement = uArgumentExpression.sourcePsi ?: return null
        return if (uArgumentExpression.isInjectionHost()) {
            ElementManipulators.getValueText(psiElement)
        } else {
            uArgumentExpression.evaluate() as? String
        }
    }

}