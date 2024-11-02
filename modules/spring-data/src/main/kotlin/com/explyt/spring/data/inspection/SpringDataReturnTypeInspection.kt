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

package com.explyt.spring.data.inspection

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.data.SpringDataBundle.message
import com.explyt.spring.data.SpringDataClasses
import com.explyt.spring.data.util.SpringDataRepositoryUtil
import com.explyt.util.TypeQuickFixUtil
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiTypes
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getParentOfType
import org.springframework.data.repository.query.parser.PartTree


class SpringDataReturnTypeInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        method: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val uClass = method.getParentOfType<UClass>() ?: return emptyArray()
        val typeParams = SpringDataRepositoryUtil.substituteRepositoryTypes(uClass.javaPsi) ?: return emptyArray()
        if (AnnotationUtil.isAnnotated(method.javaPsi, SpringDataClasses.QUERY, AnnotationUtil.CHECK_HIERARCHY)) {
            return emptyArray()
        }

        val methodName = method.getName()
        val returnType = method.returnType ?: return emptyArray()
        val returnTypeReference = method.returnTypeReference?.sourcePsi ?: return emptyArray()
        val domainClass = typeParams.psiClass
        val subject = PartTree(methodName, domainClass).subject
        val holder = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        if (subject.isCountProjection) {
            if (!SpringDataRepositoryUtil.isNumberType(returnType)) {
                holder.registerProblem(
                    returnTypeReference,
                    message("explyt.spring.data.inspection.return.type.count"),
                    *TypeQuickFixUtil.getQuickFixesReturnType(method, PsiTypes.longType())
                )
            }
        } else if (subject.isExistsProjection) {
            if (!SpringDataRepositoryUtil.isBooleanType(returnType)) {
                holder.registerProblem(
                    returnTypeReference,
                    message("explyt.spring.data.inspection.return.type.boolean"),
                    *TypeQuickFixUtil.getQuickFixesReturnType(method, PsiTypes.booleanType())
                )
            }
        } else if (subject.isDelete) {
            if (!SpringDataRepositoryUtil.isNumberType(returnType)
                && !SpringDataRepositoryUtil.isVoidType(returnType)
            ) {
                holder.registerProblem(
                    returnTypeReference,
                    message("explyt.spring.data.inspection.return.type.remove"),
                    *TypeQuickFixUtil.getQuickFixesReturnType(method, PsiTypes.voidType(), PsiTypes.longType())
                )
            }
        }

        return holder.resultsArray
    }
}