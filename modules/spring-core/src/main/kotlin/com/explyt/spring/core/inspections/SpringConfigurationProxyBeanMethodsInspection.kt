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

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.quickfix.AddAsMethodArgQuickFix
import com.explyt.util.ExplytAnnotationUtil.getUMetaAnnotation
import com.explyt.util.ExplytPsiUtil.findChildrenOfType
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiIdentifier
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.*

class SpringConfigurationProxyBeanMethodsInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
        val uClass = uMethod.getParentOfType<UClass>() ?: return null
        val surroundingClass: PsiClass = uClass.javaPsi
        if (uClass.isStatic) return null
        if (!method.isMetaAnnotatedBy(SpringCoreClasses.BEAN)) return null
        var topClass: PsiClass? = surroundingClass

        while (topClass != null) {
            val proxyBeanMethodsValue = uClass.getUMetaAnnotation(SpringCoreClasses.CONFIGURATION)
                ?.javaPsi?.let {
                    AnnotationUtil.getBooleanAttributeValue(it, "proxyBeanMethods")
                }

            if (proxyBeanMethodsValue == false) {
                return findCallsToLocalBeans(method, topClass).asSequence()
                    .mapNotNull {
                        createProblemDescriptor(
                            manager,
                            SpringCoreBundle.message("explyt.spring.inspection.configuration.proxy.incorrect"),
                            it,
                            isOnTheFly
                        )
                    }.toList().toTypedArray()
            } else if (proxyBeanMethodsValue == null //not metaAnnotated by Configuration
                && topClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)
            ) {
                return findCallsToLocalBeans(method, topClass).asSequence()
                    .mapNotNull {
                        createProblemDescriptor(
                            manager,
                            SpringCoreBundle.message("explyt.spring.inspection.configuration.light-bean.incorrect"),
                            it,
                            isOnTheFly
                        )
                    }.toList().toTypedArray()
            }
            topClass = topClass.containingClass
        }
        return null
    }

    private fun createProblemDescriptor(
        manager: InspectionManager,
        message: String,
        callExpression: UCallExpression,
        isOnTheFly: Boolean
    ): ProblemDescriptor? {
        val identifier = callExpression.methodIdentifier?.sourcePsi ?: return null
        val fixes = listOfNotNull(
            (identifier as? PsiIdentifier)
                ?.let { AddAsMethodArgQuickFix(it) }
        ).toTypedArray()

        return manager.createProblemDescriptor(
            identifier,
            identifier.getHighlightRange(),
            message,
            ProblemHighlightType.GENERIC_ERROR,
            isOnTheFly,
            *fixes
        )
    }

    private fun findCallsToLocalBeans(psiMethod: PsiMethod, surroundingClass: PsiClass): List<UCallExpression> {
        val beanMethods = surroundingClass.methods
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.BEAN) }
            .toSet()

        return psiMethod.toSourcePsi()?.findChildrenOfType<PsiElement>()?.asSequence()
            ?.mapNotNull { it.toUElement() as? UCallExpression }
            ?.filter { beanMethods.contains(it.resolve()) }
            ?.toList()
            ?: emptyList()
    }

}