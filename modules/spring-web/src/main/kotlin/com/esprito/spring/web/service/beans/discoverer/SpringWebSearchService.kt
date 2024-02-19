package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

@Service(Service.Level.PROJECT)
class SpringWebSearchService(private val project: Project) {
    private val cachedValuesManager = CachedValuesManager.getManager(project)

    fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val controllerAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.CONTROLLER, false
        )
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        return controllerAnnotations.asSequence().flatMap { searchAnnotatedClasses(it, module) }
            .flatMap { getEndpoints(it, requestMappingMah) }
            .toList()
    }

    fun getEndpointElements(urlPath: String, module: Module): List<EndpointElement> {
        val searchUrl = SpringWebUtil.simplifyUrl(urlPath)

        return searchEndpoints(module).filter { it.path == searchUrl }
    }

    private fun searchAnnotatedClasses(annotation: PsiClass, module: Module): List<PsiClass> {
        return AnnotatedElementsSearch.searchPsiClasses(annotation, module.moduleWithDependenciesScope)
            .filter { !it.isAnnotationType }
    }

    private fun getEndpoints(
        controllerPsiClass: PsiClass, requestMappingMah: MetaAnnotationsHolder
    ): List<EndpointElement> {
        val prefixes = requestMappingMah.getAnnotationMemberValues(controllerPsiClass, TARGET_VALUE)
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .ifEmpty { listOf("") }

        val result = mutableListOf<EndpointElement>()

        for (method in controllerPsiClass.allMethods) {
            if (!method.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) continue

            val annotationMemberValues = requestMappingMah.getAnnotationMemberValues(method, TARGET_VALUE)
            val values = if (annotationMemberValues.isEmpty()) {
                listOf("")
            } else {
                annotationMemberValues.mapNotNull {
                    AnnotationUtil.getStringAttributeValue(it)
                }
            }

            val requestMethods = requestMappingMah.getAnnotationMemberValues(method, TARGET_METHOD)
                .map { it.text.split('.').last() }

            for (value in values) {
                for (prefix in prefixes) {
                    result += EndpointElement(
                        SpringWebUtil.simplifyUrl("$prefix/$value"),
                        requestMethods,
                        method,
                        controllerPsiClass
                    )
                }
            }
        }
        return result
    }

    data class EndpointElement(
        val path: String,
        val requestMethods: List<String>,
        val psiElement: PsiElement,
        val containingClass: PsiClass
    )

    companion object {
        fun getInstance(project: Project): SpringWebSearchService = project.service()
        private val TARGET_VALUE = setOf("value")
        private val TARGET_METHOD = setOf("method")
    }

}