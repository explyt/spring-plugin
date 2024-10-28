package com.esprito.spring.core.inspections

import com.esprito.inspection.SpringBaseUastLocalInspectionTool
import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringCoreClasses.DEPENDS_ON
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.service.SpringSearchServiceFacade
import com.esprito.util.EspritoPsiUtil.getHighlightRange
import com.esprito.util.EspritoPsiUtil.toSourcePsi
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.ModuleUtilCore
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UDeclaration
import org.jetbrains.uast.UMethod


class SpringDependsOnBeanInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkMethod(
        uMethod: UMethod,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        return check(uMethod, manager, isOnTheFly)
    }

    override fun checkClass(
        uClass: UClass,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        return check(uClass, manager, isOnTheFly)
    }

    private fun check(
        uMember: UDeclaration,
        manager: InspectionManager,
        isOnTheFly: Boolean
    ): Array<ProblemDescriptor>? {
        val member = uMember.javaPsi ?: return null
        val module = ModuleUtilCore.findModuleForPsiElement(member) ?: return null
        val service = SpringSearchServiceFacade.getInstance(module.project)
        val beanNames = service.getAllBeanByNames(module)

        val metaHolder = SpringSearchService.getInstance(module.project).getMetaAnnotations(module, DEPENDS_ON)
        val psiAnnotation = uMember.uAnnotations.asSequence()
            .mapNotNull { it.javaPsi }
            .firstOrNull { metaHolder.contains(it) }
            ?: return null

        val annotationMemberValues = metaHolder.getAnnotationMemberValues(psiAnnotation, setOf("value"))

        return annotationMemberValues.asSequence()
            .filter {
                !beanNames.contains(
                    AnnotationUtil.getStringAttributeValue(it)
                )
            }
            .mapNotNull {
                val sourcePsi = it.toSourcePsi() ?: return@mapNotNull null

                manager.createProblemDescriptor(
                    sourcePsi,
                    sourcePsi.getHighlightRange(),
                    SpringCoreBundle.message("esprito.spring.inspection.bean.dependsOn.incorrect"),
                    ProblemHighlightType.GENERIC_ERROR,
                    isOnTheFly
                )
            }.toList().toTypedArray()
    }

}