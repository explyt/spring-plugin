/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

import com.explyt.base.LibraryClassCache
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.properties.FoldedPropertyValue
import com.explyt.spring.core.properties.references.ActuatorEndpoint
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.util.ActuatorExposure
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.Module
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * Actuator endpoints of the module, one element per operation: the ones the project declares and the built-in ones
 * its libraries bring, each carrying whether the configuration exposes it over HTTP.
 *
 * Every endpoint is listed, an unexposed one included: the exposure is read from configuration that a profile or an
 * environment variable can override, so hiding an endpoint on a static reading would hide one the running application
 * may well serve.
 *
 * Discovery is shared with `management.endpoint.<id>.*` key resolution: both answer "which endpoint ids does this
 * module declare", and two copies of that answer would disagree the first time the meta-annotation set changes.
 * A project endpoint redeclaring a built-in id is listed instead of it: Boot's own endpoint beans back off for a
 * project bean, and two endpoints sharing an id fail the application at startup.
 *
 * The path is a server-relative path, never an absolute URL: it is also the key [getEndpointElements] matches a URL
 * literal against, and a host in it survives [com.explyt.spring.web.util.SpringWebUtil.simplifyUrl] as a path segment
 * that no literal can match. It would also make the same endpoint a different element in each module that declares a
 * management port, so a shared endpoint would be listed once per consumer.
 */
class ActuatorEndpointLoader(private val project: Project) : SpringWebEndpointsLoader {

    override fun isApplicable(module: Module) =
        LibraryClassCache.searchForLibraryClass(project, SpringCoreClasses.ACTUATOR_ENDPOINT) != null

    override fun getType(): EndpointType = EndpointType.ACTUATOR

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            val trackers = ModificationTrackerManager.getInstance(project)
            // The path is read from the configuration, so a property edit invalidates the result just as code does.
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                trackers.getUastModelAndLibraryTracker(),
                trackers.getPropertyTracker()
            )
        }
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val declared = ActuatorEndpointKeys.endpointsById(module)
        val builtIn = ActuatorEndpointKeys.libraryEndpointsById(module).filterKeys { it !in declared }
        val endpoints = (declared.values + builtIn.values).flatten()
        if (endpoints.isEmpty()) return emptyList()

        // Read once: resolving each key on its own rescans every configuration file of the module and its dependencies.
        val definitions = DefinedConfigurationPropertiesSearch.getInstance(project).getAllProperties(module)
            .groupBy { it.key }
        val propertyValue = { key: String ->
            FoldedPropertyValue.choose(module, definitions[key].orEmpty())?.value?.takeIf { it.isNotBlank() }
        }
        val basePath = propertyValue(BASE_PATH_KEY) ?: DEFAULT_BASE_PATH
        val exposure = ActuatorExposure.of(module, definitions)

        return endpoints.flatMap { endpointElements(it, basePath, propertyValue, exposure.exposureOf(it.id)) }
    }

    private fun endpointElements(
        endpoint: ActuatorEndpoint,
        basePath: String,
        propertyValue: (String) -> String?,
        exposure: EndpointExposure
    ): List<EndpointElement> {
        // A JMX-only endpoint is not published over HTTP at all, so any path shown for it would be invented.
        if (endpoint.psiClass.isMetaAnnotatedBy(SpringCoreClasses.ACTUATOR_JMX_ENDPOINT)) return emptyList()

        val mappedId = propertyValue("$PATH_MAPPING_KEY.${endpoint.id}") ?: endpoint.id
        val endpointPath = joinPath(basePath, mappedId)

        val operations = endpoint.psiClass.allMethods
            .mapNotNull { operationElement(it, endpoint, endpointPath, exposure) }
        // An endpoint without operations answers nothing, but hiding it would hide the declaration too.
        return operations.ifEmpty {
            listOf(endpointElement(endpointPath, getType().name, endpoint.psiClass, endpoint, exposure))
        }
    }

    private fun operationElement(
        method: PsiMethod,
        endpoint: ActuatorEndpoint,
        endpointPath: String,
        exposure: EndpointExposure
    ): EndpointElement? {
        ProgressManager.checkCanceled()
        val httpMethod = HTTP_METHOD_BY_OPERATION.entries
            .firstOrNull { method.isMetaAnnotatedBy(it.key) }
            ?.value ?: return null

        val selectors = method.parameterList.parameters
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.ACTUATOR_SELECTOR) }
            .joinToString("") { "/{${it.name}}" }

        return endpointElement(endpointPath + selectors, httpMethod, method, endpoint, exposure)
    }

    private fun endpointElement(
        path: String,
        requestMethod: String,
        psiElement: PsiElement,
        endpoint: ActuatorEndpoint,
        exposure: EndpointExposure
    ) = EndpointElement(
        path, listOf(requestMethod), psiElement, endpoint.psiClass, null, getType(), exposure = exposure
    )


    private fun joinPath(vararg segments: String): String =
        segments.asSequence()
            .map { it.trim().trim('/') }
            .filter { it.isNotEmpty() }
            .joinToString("/", prefix = "/")
}

private const val DEFAULT_BASE_PATH = "/actuator"
private const val BASE_PATH_KEY = "management.endpoints.web.base-path"
private const val PATH_MAPPING_KEY = "management.endpoints.web.path-mapping"

private val HTTP_METHOD_BY_OPERATION = mapOf(
    SpringCoreClasses.ACTUATOR_READ_OPERATION to "GET",
    SpringCoreClasses.ACTUATOR_WRITE_OPERATION to "POST",
    SpringCoreClasses.ACTUATOR_DELETE_OPERATION to "DELETE"
)
