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
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isPrivate
import com.intellij.codeInsight.daemon.impl.quickfix.MethodReturnTypeFix
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.isInheritorOf
import com.intellij.psi.*
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod


class SpringAsyncReturnTypeInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val psiClass = uClass.javaPsi
        val isClassAsync = psiClass.isMetaAnnotatedBy(SpringCoreClasses.ASYNC)

        val futurePsiClass = lazy {
            val javaPsiFacade = JavaPsiFacade.getInstance(psiClass.project)
            javaPsiFacade.findClass(SpringCoreClasses.FUTURE, psiClass.resolveScope)
        }

        return uClass.methods.asSequence()
            .filter { !it.javaPsi.isPrivate }
            .flatMapTo(mutableListOf()) {
                checkMethod(it, manager, isOnTheFly, isClassAsync, futurePsiClass.value)
            }
            .toTypedArray()
    }

    private fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean,
        isClassAsync: Boolean,
        genericPsiClass: PsiClass?
    ): List<ProblemDescriptor> {
        if (genericPsiClass == null) return emptyList()
        if (uMethod.returnType == PsiTypes.voidType()) return emptyList()
        val psiIdentifier = uMethod.uastAnchor?.sourcePsi ?: return emptyList()
        val psiMethod = uMethod.javaPsi
        if (!isClassAsync && !psiMethod.isMetaAnnotatedBy(SpringCoreClasses.ASYNC)) return emptyList()
        val returnType = uMethod.returnType ?: return emptyList()
        if (returnType.isInheritorOf(SpringCoreClasses.FUTURE)) return emptyList()

        return listOf(
            manager.createProblemDescriptor(
                psiIdentifier,
                SpringCoreBundle.message("explyt.spring.inspection.async.signature.incorrect"),
                isOnTheFly,
                arrayOf(
                    MethodReturnTypeFix(psiMethod, createReturnType(genericPsiClass, psiMethod, returnType), true)
                ),
                ProblemHighlightType.WARNING,
            )
        )
    }

    private fun createReturnType(genericPsiClass: PsiClass, psiMethod: PsiMethod, returnType: PsiType): PsiType {
        return JavaPsiFacade.getInstance(psiMethod.project)
            .elementFactory.createType(genericPsiClass, boxedIfPrimitive(returnType, psiMethod))
    }

    private fun boxedIfPrimitive(returnType: PsiType, psiMethod: PsiMethod): PsiType {
        if (returnType !is PsiPrimitiveType) return returnType

        return returnType.getBoxedType(psiMethod) ?: returnType
    }

}