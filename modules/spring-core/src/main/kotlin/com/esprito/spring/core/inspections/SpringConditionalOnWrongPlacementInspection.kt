package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.BEAN
import com.esprito.spring.core.SpringCoreClasses.COMPONENT
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_BEAN
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_CLASS
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_MISSING_BEAN
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_MISSING_CLASS
import com.esprito.spring.core.SpringCoreClasses.CONDITIONAL_ON_PROPERTY
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.esprito.util.EspritoPsiUtil.toSourcePsi
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod


class SpringConditionalOnWrongPlacementInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uMethod.javaPsi.isMetaAnnotatedBy(BEAN)) {
            return null
        }
        return check(uMethod, manager, isOnTheFly)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        if (uClass.isAnnotationType) {
            return null
        }
        if (uClass.javaPsi.isMetaAnnotatedBy(COMPONENT)) {
            return null
        }
        return check(uClass, manager, isOnTheFly)
    }

    private fun check(
        uMember: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val member = uMember.javaPsi ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return null
        val service = SpringSearchService.getInstance(module.project)

        val annotationsHolders = setOf(
            CONDITIONAL_ON_PROPERTY,
            CONDITIONAL_ON_CLASS,
            CONDITIONAL_ON_MISSING_CLASS,
            CONDITIONAL_ON_BEAN,
            CONDITIONAL_ON_MISSING_BEAN
        ).map { service.getMetaAnnotations(module, it) }

        return uMember.uAnnotations.asSequence()
            .mapNotNull { it.javaPsi }
            .filter { annotation ->
                annotationsHolders.any { it.contains(annotation) }
            }
            .mapNotNull { annotation ->
                val sourcePsi = annotation.toSourcePsi() ?: return@mapNotNull null

                manager.createProblemDescriptor(
                    sourcePsi,
                    sourcePsi.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.conditionalOn.wrongPlacement"),
                    ProblemHighlightType.WARNING,
                    isOnTheFly
                )

            }
            .toList()
            .toTypedArray()
    }

}