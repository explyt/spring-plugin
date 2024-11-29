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
import com.explyt.spring.core.inspections.quickfix.KotlinObjectToClassQuickFix
import com.explyt.spring.core.service.SpringSearchUtils
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UClass


class SpringKotlinObjectInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        if (uClass.lang != KotlinLanguage.INSTANCE) return emptyArray()

        val sourcePsi = uClass.sourcePsi ?: return emptyArray()
        val file = sourcePsi.containingFile ?: return emptyArray()
        val module = ModuleUtilCore.findModuleForFile(file) ?: return emptyArray()
        SpringSearchUtils.findUAnnotation(module, uClass.uAnnotations, SpringCoreClasses.COMPONENT)
            ?: return emptyArray()

        val objectDeclaration = (sourcePsi as? KtObjectDeclaration) ?: return emptyArray()
        val objectKeywordPsi = objectDeclaration.getObjectKeyword() ?: return emptyArray()

        return arrayOf(
            manager.createProblemDescriptor(
                objectKeywordPsi,
                SpringCoreBundle.message("explyt.spring.inspection.kotlin.object.title"),
                isOnTheFly,
                arrayOf(KotlinObjectToClassQuickFix(objectDeclaration)),
                ProblemHighlightType.WARNING
            )
        )
    }
}