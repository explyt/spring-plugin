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