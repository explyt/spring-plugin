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
import com.explyt.spring.core.SpringCoreClasses.RETENTION
import com.explyt.spring.core.SpringCoreClasses.RETENTION_POLICY
import com.explyt.spring.core.SpringCoreClasses.TARGET
import com.explyt.spring.core.inspections.quickfix.RewriteAnnotationQuickFix
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.UClass


class SpringMetaAnnotationWithoutRuntimeInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        aClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (!aClass.isAnnotationType) return null
        val psiClass = aClass.javaPsi
        val identity = getIdentifyingElement(aClass) ?: return null

        if (!isMetaAnnotatedWithSpringAnnotation(psiClass.annotations.toList())) return null

        val retention = psiClass.getAnnotation(RETENTION)
            ?: return createProblemDescriptor(identity, psiClass, manager, isOnTheFly)

        val retentionPolicy = AnnotationUtil.findDeclaredAttribute(retention, "value")?.value
            ?: return null

        return if (retentionPolicy.reference?.canonicalText?.contains(RETENTION_POLICY_RUNTIME) == true) {
            null
        } else {
            createProblemDescriptor(identity, psiClass, manager, isOnTheFly)
        }
    }

    private fun isMetaAnnotatedWithSpringAnnotation(psiAnnotations: Collection<PsiAnnotation>): Boolean {
        for (psiAnnotation in psiAnnotations) {
            val annotationQN = psiAnnotation.qualifiedName ?: continue
            if (annotationQN in setOf(TARGET, RETENTION)) continue

            if (annotationQN.startsWith(SPRING_PREFIX)) return true

            val nestedAnnotations = psiAnnotation.resolveAnnotationType()
                ?.annotations
                ?.filter { it.qualifiedName != annotationQN }
                ?: continue
            if (isMetaAnnotatedWithSpringAnnotation(nestedAnnotations)) return true
        }
        return false
    }


    private fun createProblemDescriptor(
        psiElement: PsiElement,
        psiClass: PsiClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        return arrayOf(
            manager.createProblemDescriptor(
                psiElement,
                SpringCoreBundle.message("explyt.spring.inspection.retention.incorrect"),
                RewriteAnnotationQuickFix(
                    "$RETENTION($RETENTION_POLICY.RUNTIME)",
                    psiClass,
                    RETENTION
                ),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly
            )
        )
    }

    private fun getIdentifyingElement(aClass: UClass): PsiElement? {
        return (aClass.sourcePsi as? PsiNameIdentifierOwner)
            ?.identifyingElement
            ?.navigationElement
    }

    companion object {
        const val SPRING_PREFIX = "org.springframework."
        const val RETENTION_POLICY_RUNTIME = "RetentionPolicy.RUNTIME"
    }

}