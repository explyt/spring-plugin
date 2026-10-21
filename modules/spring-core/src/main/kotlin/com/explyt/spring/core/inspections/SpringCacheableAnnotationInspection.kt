/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections

import com.explyt.inspection.SpringBaseUastLocalInspectionTool
import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringCoreClasses.CACHE_KEY_GENERATOR
import com.explyt.spring.core.SpringCoreClasses.CACHE_MANAGER
import com.explyt.spring.core.SpringCoreClasses.CACHE_RESOLVER
import com.explyt.spring.core.service.SpringBeanOwner
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.explyt.util.ExplytPsiUtil.isPublic
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.PsiClass
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod


class SpringCacheableAnnotationInspection : SpringBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val holder = ProblemsHolder(manager, uClass.javaPsi.containingFile, isOnTheFly)
        checkCacheParams(uClass.uAnnotations, holder, uClass.javaPsi)
        return holder.resultsArray
    }

    override fun checkMethod(
        method: UMethod, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val holder = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        val cacheAnnotationIsFound = checkCacheParams(method.uAnnotations, holder, method.javaPsi.containingClass)

        if (cacheAnnotationIsFound && !method.isPublic) {
            val sourcePsi = method.uastAnchor?.sourcePsi ?: return holder.resultsArray
            holder.registerProblem(sourcePsi, message("explyt.spring.inspection.cache.public"))
        }
        return holder.resultsArray
    }

    private fun checkCacheParams(uAnnotations: List<UAnnotation>, holder: ProblemsHolder, owner: PsiClass?): Boolean {
        val cacheAnnotations = uAnnotations.filter { it.isCacheAnnotation() }
        if (cacheAnnotations.isNotEmpty() && owner != null && isProcessedBySpring(owner)) {
            cacheAnnotations.forEach { checkCacheParams(it, holder) }
        }
        return cacheAnnotations.isNotEmpty()
    }

    private fun UAnnotation.isCacheAnnotation(): Boolean =
        javaPsi?.isMetaAnnotatedByOrSelf(SpringCoreClasses.ANNOTATIONS_CACHE) == true

    private fun isProcessedBySpring(owner: PsiClass): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(owner) ?: return false
        return SpringBeanOwner.isProcessedBySpring(module, owner)
    }

    private fun checkCacheParams(uAnnotation: UAnnotation, holder: ProblemsHolder) {
        processCacheParameter("cacheResolver", uAnnotation, holder)
        processCacheParameter("cacheManager", uAnnotation, holder)
        processCacheParameter("keyGenerator", uAnnotation, holder)
    }

    private fun processCacheParameter(parameterName: String, uAnnotation: UAnnotation, holder: ProblemsHolder) {
        val findAttributeValue = uAnnotation.findAttributeValue(parameterName)
        val sourcePsi = findAttributeValue?.sourcePsi ?: return
        val valueText = ElementManipulators.getValueText(sourcePsi)
        if (valueText.isBlank()) return

        val className = getClassNameByParameterName(parameterName)
        val module = ModuleUtilCore.findModuleForPsiElement(sourcePsi) ?: return
        val service = SpringSearchServiceFacade.getInstance(module.project)
        val beans = service.getAllBeanByNames(module)[valueText]
        if (beans.isNullOrEmpty()) {
            holder.registerProblem(
                sourcePsi,
                message("explyt.spring.inspection.cache.bean.parameter", parameterName, className)
            )
            return
        }
        val cacheResolverBean = beans.takeIf { beans.size == 1 }?.get(0) ?: return

        if (!InheritanceUtil.isInheritor(cacheResolverBean.psiClass, className)) {
            holder.registerProblem(
                sourcePsi,
                message("explyt.spring.inspection.cache.bean.parameter", parameterName, className)
            )
        }
    }

    private fun getClassNameByParameterName(parameterName: String): String {
        return when (parameterName) {
            "cacheResolver" -> CACHE_RESOLVER
            "cacheManager" -> CACHE_MANAGER
            "keyGenerator" -> CACHE_KEY_GENERATOR
            else -> throw IllegalArgumentException()
        }
    }
}