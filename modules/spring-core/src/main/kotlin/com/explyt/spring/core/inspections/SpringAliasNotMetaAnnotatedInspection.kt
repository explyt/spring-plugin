/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.AliasUtils
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.toSourcePsi
import com.intellij.codeInsight.intention.AddAnnotationModCommandAction
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.psi.PsiClass
import com.intellij.psi.util.parentOfType
import org.jetbrains.uast.UMethod


class SpringAliasNotMetaAnnotatedInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val method = uMethod.javaPsi
        val uAliasAnnotation = uMethod.uAnnotations
            .firstOrNull { it.qualifiedName == SpringCoreClasses.ALIAS_FOR } ?: return null
        val aliasAnnotation = uAliasAnnotation.javaPsi ?: return null
        val aliasedClassQn =
            AliasUtils.getAliasedClass(aliasAnnotation)
                ?.takeIf { it.isAnnotationType }
                ?.qualifiedName
                ?: return null

        val parentClass = method.parentOfType<PsiClass>() ?: return null
        val isMetaAnnotationFound = parentClass.qualifiedName == aliasedClassQn
                || parentClass.isMetaAnnotatedBy(aliasedClassQn)

        if (isMetaAnnotationFound) return null

        val annotationMemberValue = aliasAnnotation.findAttributeValue("annotation")
            .toSourcePsi() ?: return null

        val quickFix = LocalQuickFix.from(AddAnnotationModCommandAction(aliasedClassQn, parentClass)) ?: return null
        return arrayOf(
            manager.createProblemDescriptor(
                annotationMemberValue,
                annotationMemberValue.getHighlightRange(),
                SpringCoreBundle.message("explyt.spring.inspection.alias.annotation"),
                ProblemHighlightType.GENERIC_ERROR,
                isOnTheFly,
                quickFix
            )
        )
    }

}