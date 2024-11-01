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
import com.explyt.spring.core.SpringCoreClasses.ANNOTATIONS_CACHE
import com.explyt.util.ExplytPsiUtil.isEqualOrInheritor
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass

class SpringCacheAnnotationsOnInterfaceInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val uClass = uMethod.getContainingUClass() ?: return null
        if (!uClass.isInterface) return null
        val psiClass = uClass.javaPsi
        if (isImplicitlySubclassed(psiClass)) return null
        val psiMethod = uMethod.javaPsi

        if (!psiMethod.isMetaAnnotatedBy(ANNOTATIONS_CACHE)) return null
        val identifyingElement = (uMethod.sourcePsi as? PsiNameIdentifierOwner)?.identifyingElement ?: return null

        return arrayOf(
            manager.createProblemDescriptor(
                identifyingElement,
                SpringCoreBundle.message("explyt.spring.inspection.cache.interface.method"),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uClass.isAnnotationType || !uClass.isInterface) return null
        val psiClass = uClass.javaPsi
        if (isImplicitlySubclassed(psiClass)) return null

        if (!psiClass.isMetaAnnotatedBy(ANNOTATIONS_CACHE)) return null
        val identifyingElement = (uClass.sourcePsi as? PsiNameIdentifierOwner)?.identifyingElement ?: return null

        return arrayOf(
            manager.createProblemDescriptor(
                identifyingElement,
                SpringCoreBundle.message("explyt.spring.inspection.cache.interface.class"),
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    private fun isImplicitlySubclassed(psiClass: PsiClass): Boolean {

        return psiClass.isEqualOrInheritor("org.springframework.data.repository.Repository")
                || psiClass.isMetaAnnotatedBy(
            listOf(
                SpringCoreClasses.NETFLIX_FEIGN_CLIENT,
                SpringCoreClasses.OPEN_FEIGN_CLIENT
            )
        )
    }

}