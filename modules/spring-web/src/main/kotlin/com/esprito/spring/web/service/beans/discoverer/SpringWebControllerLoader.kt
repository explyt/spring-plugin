package com.esprito.spring.web.service.beans.discoverer

import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class SpringWebControllerLoader(private val project: Project) : SpringWebEndpointsLoader {
    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
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

    companion object {
        private val TARGET_VALUE = setOf("value")
        private val TARGET_METHOD = setOf("method")
    }
}