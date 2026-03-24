/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
import com.intellij.psi.PsiJvmModifiersOwner
import com.intellij.psi.PsiNameIdentifierOwner
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getContainingUClass

class SpringInterfaceCacheAnnotationsInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val uClass = uMethod.getContainingUClass() ?: return null
        if (!uClass.isInterface) return null
        val psiClass = uClass.javaPsi
        if (isSubclassedBuSpring(psiClass)) return null

        return check(
            uMethod,
            manager,
            isOnTheFly,
            SpringCoreBundle.message("explyt.spring.inspection.cache.interface.method")
        )
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uClass.isAnnotationType || !uClass.isInterface) return null
        val psiClass = uClass.javaPsi
        if (isSubclassedBuSpring(psiClass)) return null

        return check(
            uClass,
            manager,
            isOnTheFly,
            SpringCoreBundle.message("explyt.spring.inspection.cache.interface.class")
        )
    }

    private fun check(
        uElement: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        errorMessage: String
    ): Array<ProblemDescriptor>? {
        val psiElement = uElement.javaPsi as? PsiJvmModifiersOwner ?: return null

        if (!psiElement.isMetaAnnotatedBy(ANNOTATIONS_CACHE)) return null
        val identifyingElement = (uElement.sourcePsi as? PsiNameIdentifierOwner)?.identifyingElement ?: return null

        return arrayOf(
            manager.createProblemDescriptor(
                identifyingElement,
                errorMessage,
                isOnTheFly,
                emptyArray(),
                ProblemHighlightType.GENERIC_ERROR_OR_WARNING
            )
        )
    }

    private fun isSubclassedBuSpring(psiClass: PsiClass): Boolean {

        return psiClass.isEqualOrInheritor("org.springframework.data.repository.Repository")
                || psiClass.isMetaAnnotatedBy(
            listOf(
                SpringCoreClasses.NETFLIX_FEIGN_CLIENT,
                SpringCoreClasses.OPEN_FEIGN_CLIENT
            )
        )
    }

}