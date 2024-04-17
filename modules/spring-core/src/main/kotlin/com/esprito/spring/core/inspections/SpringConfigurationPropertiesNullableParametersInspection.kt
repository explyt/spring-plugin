package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
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
            if (node.getContainingUClass()?.javaPsi
                    ?.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION_PROPERTIES) != true
            ) return true
            if (ktParameter.typeReference?.typeElement is KtNullableType) return true

            problemsHolder.registerProblem(
                problemsHolder.manager.createProblemDescriptor(
                    ktParameter,
                    ktParameter.getHighlightRange(),
                    SpringCoreBundle.message(
                        "esprito.spring.inspection.kotlin.constructor.nullable"
                    ),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            )

            return super.visitParameter(node)
        }

    }

}