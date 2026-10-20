/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.providers.AbstractComponentLineMarker
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.LocalQuickFix
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiWhiteSpace
import org.jetbrains.uast.UClass

class SpringAbstractComponentInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val psiClass = uClass.javaPsi
        if (!AbstractComponentLineMarker.isAbstractComponent(psiClass)) return emptyArray()
        val module = ModuleUtilCore.findModuleForPsiElement(psiClass) ?: return emptyArray()
        val stereotype = SpringSearchUtils.findUAnnotation(module, uClass.uAnnotations, SpringCoreClasses.COMPONENT)
            ?: return emptyArray()
        if (stereotype.javaPsi?.isMetaAnnotatedByOrSelf(typeHierarchyStereotypes) != false) return emptyArray()
        val anchor = stereotype.sourcePsi ?: return emptyArray()
        val annotationName = stereotype.qualifiedName?.substringAfterLast('.') ?: return emptyArray()

        return arrayOf(
            manager.createProblemDescriptor(
                anchor,
                message(problemKey(psiClass, module), annotationName),
                isOnTheFly,
                arrayOf(RemoveStereotypeAnnotationFix()),
                ProblemHighlightType.WARNING
            )
        )
    }

    private fun problemKey(abstractClass: PsiClass, module: Module): String {
        val facade = SpringSearchServiceFacade.getInstance(module.project)
        val beans = facade.getAllActiveBeans(module)
        val factories = facade.getActiveGenericBeanFactoryMethods(module)
        return if (AbstractComponentLineMarker.implementationsOf(abstractClass, beans, factories).isEmpty())
            "explyt.spring.inspection.abstract.component.no.bean"
        else
            "explyt.spring.inspection.abstract.component.subclass.bean"
    }

    private companion object {
        val typeHierarchyStereotypes = listOf(
            SpringCoreClasses.CONTROLLER,
            SpringCoreClasses.CONTROLLER_ADVICE,
            SpringCoreClasses.REPOSITORY
        )
    }
}

private class RemoveStereotypeAnnotationFix : LocalQuickFix {

    override fun getFamilyName(): String = message("explyt.spring.inspection.abstract.component.fix")

    override fun applyFix(project: Project, descriptor: ProblemDescriptor) {
        val annotation = descriptor.psiElement?.takeIf { it.isValid } ?: return
        val trailingWhiteSpace = annotation.nextSibling as? PsiWhiteSpace
        annotation.delete()
        trailingWhiteSpace?.takeIf { it.isValid }?.delete()
    }
}
