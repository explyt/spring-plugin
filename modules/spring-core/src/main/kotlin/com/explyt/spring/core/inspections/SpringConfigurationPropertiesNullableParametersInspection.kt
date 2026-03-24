/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.CONSTRUCTOR_BINDING
import com.explyt.util.AddParameterMethodAnnotationKotlinFix
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UClass

class SpringConfigurationPropertiesNullableParametersInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<out ProblemDescriptor?> {
        if (uClass.lang != KotlinLanguage.INSTANCE) return emptyArray()
        val javaPsi = uClass.javaPsi
        if (!javaPsi.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) return emptyArray()
        if (javaPsi.constructors.any { it?.isMetaAnnotatedBy(CONSTRUCTOR_BINDING) == true }) return emptyArray()

        val problems = mutableListOf<ProblemDescriptor>()
        for (method in uClass.methods) {
            if (!method.isConstructor) continue
            for (parameter in method.uastParameters) {
                val ktParameter = parameter.sourcePsi as? KtParameter ?: continue
                if (ktParameter.hasDefaultValue()) continue
                if (ktParameter.typeReference?.typeElement is KtNullableType) continue
                problems += manager.createProblemDescriptor(
                    ktParameter,
                    ktParameter.getHighlightRange(),
                    SpringCoreBundle.message("explyt.spring.inspection.kotlin.constructor.nullable"),
                    ProblemHighlightType.ERROR,
                    isOnTheFly,
                    AddParameterMethodAnnotationKotlinFix(CONSTRUCTOR_BINDING)
                )
            }
        }
        return problems.toTypedArray()
    }
}