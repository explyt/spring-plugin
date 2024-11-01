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
import com.explyt.util.ExplytPsiUtil.getHighlightRange
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.psi.PsiElementVisitor
import com.intellij.uast.UastVisitorAdapter
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getContainingUClass
import org.jetbrains.uast.visitor.AbstractUastNonRecursiveVisitor

class SpringConfigurationPropertiesNullableParametersInspection : SpringBaseUastLocalInspectionTool() {

    override fun buildVisitor(holder: ProblemsHolder, isOnTheFly: Boolean): PsiElementVisitor {
        return UastVisitorAdapter(ConstructorParameterVisitor(holder, isOnTheFly), true)
    }

    private class ConstructorParameterVisitor(
        private val problemsHolder: ProblemsHolder, private val isOnTheFly: Boolean
    ) : AbstractUastNonRecursiveVisitor() {

        override fun visitParameter(node: UParameter): Boolean {
            if (node.lang != KotlinLanguage.INSTANCE) return true
            if (node.isFinal) return true
            val ktParameter = node.sourcePsi as? KtParameter ?: return true
            if (ktParameter.hasDefaultValue()) return true
            val psiClass = node.getContainingUClass()?.javaPsi ?: return true
            if (!psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES)) return true
            if (ktParameter.typeReference?.typeElement is KtNullableType) return true
            if (psiClass.constructors.firstOrNull()
                    ?.isMetaAnnotatedBy(SpringCoreClasses.CONSTRUCTOR_BINDING) == true
            ) return true

            problemsHolder.registerProblem(
                problemsHolder.manager.createProblemDescriptor(
                    ktParameter,
                    ktParameter.getHighlightRange(),
                    SpringCoreBundle.message(
                        "explyt.spring.inspection.kotlin.constructor.nullable"
                    ),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            )

            return super.visitParameter(node)
        }

    }

}