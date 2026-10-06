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
import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.ActuatorAccess
import com.explyt.spring.web.util.ActuatorExposure
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.explyt.util.ExplytPsiUtil.getMetaAnnotation
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

/**
 * Actuator endpoints of the module, one element per operation: the ones the project declares and the built-in ones
 * its libraries bring, each carrying whether the configuration exposes it over HTTP.
 *
 * Every endpoint is listed, an unexposed one included: the exposure is read from configuration that a profile or an
 * environment variable can override, so hiding an endpoint on a static reading would hide one the running application
 * may well serve. The access the configuration grants is carried the same way, per operation: under `READ_ONLY` Boot
 * serves an endpoint's read operations alone, so its other operations read `NONE`.
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
        val application = applicationClassOf(module)
        val declared = if (application != null) ActuatorEndpointKeys.endpointsById(module) else ownEndpointsById(module)
        val builtIn = if (application != null) {
            ActuatorEndpointKeys.libraryEndpointsById(module).filterKeys { it !in declared }
        } else {
            emptyMap()
        }
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
        val access = ActuatorAccess.of(module, definitions)
        val requestMappingMah by lazy { MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING) }

        return endpoints.flatMap {
            val gates = Gates(exposure.exposureOf(it.id), access.accessOf(it), application)
            endpointElements(it, basePath, propertyValue, gates) { requestMappingMah }
        }
    }

    /**
     * The endpoints a module without an application lists: only the ones it declares itself, and none at all once an
     * application reaches it. An application decides the exposure and access of every endpoint its context holds, so
     * the library's own reading under its defaults would contradict that verdict. A library no application reaches
     * keeps its endpoints, under its own configuration, as the only reading there is.
     */
    private fun ownEndpointsById(module: Module): Map<String, List<ActuatorEndpoint>> {
        if (isReachedByApplication(module)) return emptyMap()
        return ActuatorEndpointKeys.endpointsById(module)
            .mapValues { (_, endpoints) -> endpoints.filter { declaringModuleOf(it) == module } }
            .filterValues { it.isNotEmpty() }
    }

    private fun declaringModuleOf(endpoint: ActuatorEndpoint): Module? =
        ModuleUtilCore.findModuleForPsiElement(endpoint.psiClass)

    private fun isReachedByApplication(module: Module): Boolean {
        val moduleManager = ModuleManager.getInstance(project)
        val visited = mutableSetOf(module)
        val pending = ArrayDeque(moduleManager.getModuleDependentModules(module))
        while (pending.isNotEmpty()) {
            val dependent = pending.removeFirst()
            if (!visited.add(dependent)) continue
            if (applicationClassOf(dependent) != null) return true
            pending += moduleManager.getModuleDependentModules(dependent)
        }
        return false
    }

    /**
     * The Spring Boot application [module] declares, if any. Built-in endpoints belong to the context an application
     * starts, so only its module lists them, under its configuration: a library module that merely has the Actuator
     * jar would list them again under the defaults, contradicting the application's verdict. A production application
     * outranks one in test sources, and the qualified name breaks the remaining ties, so the answer is stable.
     */
    private fun applicationClassOf(module: Module): PsiClass? {
        val fileIndex = ProjectFileIndex.getInstance(project)
        return PackageScanService.getInstance(project).getSpringBootAppAnnotations()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, module.moduleScope).findAll() }
            .minWithOrNull(
                compareBy<PsiClass> { psiClass ->
                    psiClass.containingFile?.virtualFile?.let { fileIndex.isInTestSourceContent(it) } ?: false
                }.thenBy { it.qualifiedName.orEmpty() }
            )
    }

    /** The two independent conditions under which Boot serves an endpoint, and the application whose context holds it. */
    private class Gates(val exposure: EndpointExposure, val access: EndpointAccess, val application: PsiClass?)

    private fun endpointElements(
        endpoint: ActuatorEndpoint,
        basePath: String,
        propertyValue: (String) -> String?,
        gates: Gates,
        requestMappingMah: () -> MetaAnnotationsHolder,
    ): List<EndpointElement> {
        // A JMX-only endpoint is not published over HTTP at all, so any path shown for it would be invented.
        if (endpoint.psiClass.isMetaAnnotatedBy(SpringCoreClasses.ACTUATOR_JMX_ENDPOINT)) return emptyList()

        val mappedId = propertyValue("$PATH_MAPPING_KEY.${endpoint.id}") ?: endpoint.id
        val endpointPath = joinPath(basePath, mappedId)

        val operations = if (endpoint.psiClass.isMetaAnnotatedBy(CONTROLLER_ENDPOINTS)) {
            mappingElements(endpoint, endpointPath, gates, requestMappingMah())
        } else {
            endpoint.psiClass.allMethods.mapNotNull { operationElement(it, endpoint, endpointPath, gates) }
        }
        // An endpoint declaring no operation - a servlet endpoint, or one that declares none at all - stays listed so its
        // declaration does, and names no verb because it declares none.
        return operations.ifEmpty {
            listOf(endpointElement(endpointPath, emptyList(), endpoint.psiClass, endpoint, gates, gates.access))
        }
    }

    /**
     * The operations of a controller endpoint: Boot serves its `@RequestMapping` methods under the endpoint path the
     * way Spring MVC serves a controller's, and forbids it from declaring Actuator operations.
     */
    private fun mappingElements(
        endpoint: ActuatorEndpoint,
        endpointPath: String,
        gates: Gates,
        requestMappingMah: MetaAnnotationsHolder,
    ): List<EndpointElement> = endpoint.psiClass.allMethods
        .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
        .flatMap { method ->
            ProgressManager.checkCanceled()
            val mapping = SpringWebUtil.requestMappingOf(method, requestMappingMah)
            // Boot's `ControllerEndpointHandlerMapping` keeps a read-only controller endpoint's GET and HEAD mappings
            // alone, and narrows a mapping naming no method to those two.
            val isRead = mapping.methods.isNotEmpty() && mapping.methods.all { it in READ_ONLY_REQUEST_METHODS }
            val access = ActuatorAccess.ofOperation(gates.access, isRead)
            mapping.paths.map {
                endpointElement(joinPath(endpointPath, it), mapping.methods, method, endpoint, gates, access)
            }
        }

    private fun operationElement(
        method: PsiMethod,
        endpoint: ActuatorEndpoint,
        endpointPath: String,
        gates: Gates,
    ): EndpointElement? {
        ProgressManager.checkCanceled()
        val httpMethod = HTTP_METHOD_BY_OPERATION.entries
            .firstOrNull { method.isMetaAnnotatedBy(it.key) }
            ?.value ?: return null

        val selectors = method.parameterList.parameters
            .filter { it.isMetaAnnotatedBy(SpringCoreClasses.ACTUATOR_SELECTOR) }
            .joinToString("") { "/{${it.name}}" }

        val produces = method.getMetaAnnotation(OPERATION_BY_METHOD.getValue(httpMethod)).getStringMemberValues(PRODUCES)

        val access = ActuatorAccess.ofOperation(gates.access, isRead = httpMethod == READ_METHOD)
        return endpointElement(
            endpointPath + selectors, listOf(httpMethod), method, endpoint, gates, access, produces.toList()
        )
    }

    private fun endpointElement(
        path: String,
        requestMethods: List<String>,
        psiElement: PsiElement,
        endpoint: ActuatorEndpoint,
        gates: Gates,
        access: EndpointAccess,
        produces: List<String> = emptyList(),
    ) = EndpointElement(
        path, requestMethods, psiElement, endpoint.psiClass, null, getType(),
        exposure = gates.exposure, produces = produces, access = access, application = gates.application
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

private const val PRODUCES = "produces"
private const val READ_METHOD = "GET"
private val READ_ONLY_REQUEST_METHODS = setOf("GET", "HEAD")

private val HTTP_METHOD_BY_OPERATION = mapOf(
    SpringCoreClasses.ACTUATOR_READ_OPERATION to "GET",
    SpringCoreClasses.ACTUATOR_WRITE_OPERATION to "POST",
    SpringCoreClasses.ACTUATOR_DELETE_OPERATION to "DELETE"
)

private val OPERATION_BY_METHOD = HTTP_METHOD_BY_OPERATION.entries.associate { (operation, method) -> method to operation }

private val CONTROLLER_ENDPOINTS = listOf(
    SpringCoreClasses.ACTUATOR_CONTROLLER_ENDPOINT,
    SpringCoreClasses.ACTUATOR_REST_CONTROLLER_ENDPOINT,
)
