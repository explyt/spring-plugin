package com.esprito.spring.core.inspections

import com.esprito.spring.core.SpringCoreBundle.message
import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringCoreClasses.CACHE_KEY_GENERATOR
import com.esprito.spring.core.SpringCoreClasses.CACHE_MANAGER
import com.esprito.spring.core.SpringCoreClasses.CACHE_RESOLVER
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedByOrSelf
import com.esprito.util.EspritoPsiUtil.isPublic
import com.intellij.codeInspection.AbstractBaseUastLocalInspectionTool
import com.intellij.codeInspection.InspectionManager
import com.intellij.codeInspection.ProblemDescriptor
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.psi.ElementManipulators
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UMethod


class SpringCacheableAnnotationInspection : AbstractBaseUastLocalInspectionTool() {

    override fun checkClass(
        uClass: UClass, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val holder = ProblemsHolder(manager, uClass.javaPsi.containingFile, isOnTheFly)
        checkCacheParams(uClass.uAnnotations, holder)
        return holder.resultsArray
    }

    override fun checkMethod(
        method: UMethod, manager: InspectionManager, isOnTheFly: Boolean
    ): Array<ProblemDescriptor> {
        val holder = ProblemsHolder(manager, method.javaPsi.containingFile, isOnTheFly)
        val cacheAnnotationIsFound = checkCacheParams(method.uAnnotations, holder)

        if (cacheAnnotationIsFound && !method.isPublic) {
            val sourcePsi = method.uastAnchor?.sourcePsi ?: return holder.resultsArray
            holder.registerProblem(sourcePsi, message("esprito.spring.inspection.cache.public"))
        }
        return holder.resultsArray
    }

    private fun checkCacheParams(uAnnotations: List<UAnnotation>, holder: ProblemsHolder): Boolean {
        return uAnnotations.map { checkCacheParams(it, holder) }.find { it } == true
    }

    /**
     * return true if one of cache annotations is found.
     */
    private fun checkCacheParams(uAnnotation: UAnnotation, holder: ProblemsHolder): Boolean {
        val psiAnnotation = uAnnotation.javaPsi ?: return false
        if (!psiAnnotation.isMetaAnnotatedByOrSelf(SpringCoreClasses.ANNOTATIONS_CACHE)) return false
        processCacheParameter("cacheResolver", uAnnotation, holder)
        processCacheParameter("cacheManager", uAnnotation, holder)
        processCacheParameter("keyGenerator", uAnnotation, holder)
        return true
    }

    private fun processCacheParameter(parameterName: String, uAnnotation: UAnnotation, holder: ProblemsHolder) {
        val findAttributeValue = uAnnotation.findAttributeValue(parameterName)
        val sourcePsi = findAttributeValue?.sourcePsi ?: return
        val valueText = ElementManipulators.getValueText(sourcePsi)
        if (valueText.isBlank()) return

        val className = getClassNameByParameterName(parameterName)
        val module = ModuleUtilCore.findModuleForPsiElement(sourcePsi) ?: return
        val service = SpringSearchService.getInstance(module.project)
        val beans = service.getAllBeanByNames(module)[valueText]
        if (beans.isNullOrEmpty()) {
            holder.registerProblem(
                sourcePsi,
                message("esprito.spring.inspection.cache.bean.parameter", parameterName, className)
            )
            return
        }
        val cacheResolverBean = beans.takeIf { beans.size == 1 }?.get(0) ?: return

        if (!InheritanceUtil.isInheritor(cacheResolverBean.psiClass, className)) {
            holder.registerProblem(
                sourcePsi,
                message("esprito.spring.inspection.cache.bean.parameter", parameterName, className)
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