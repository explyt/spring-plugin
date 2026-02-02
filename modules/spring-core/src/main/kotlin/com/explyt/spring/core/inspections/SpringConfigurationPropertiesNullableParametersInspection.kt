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