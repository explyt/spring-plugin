/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.UastUtil.getArgumentValueAsEnumName
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.loader.*
import com.explyt.spring.web.tracker.OpenApiLanguagesModificationTracker
import com.explyt.spring.web.util.ApplicationBasePath
import com.explyt.spring.web.util.EndpointUrlMatcher
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.PATHS
import com.explyt.spring.web.util.SpringWebUtil.REQUEST_METHODS
import com.explyt.spring.web.util.SpringWebUtil.REQUEST_METHODS_WITH_TYPE
import com.explyt.spring.web.util.SpringWebUtil.getUrlTemplateIndex
import com.explyt.spring.web.util.TestRequestReceivers
import com.explyt.spring.web.util.UrlArgumentText
import com.explyt.util.CacheUtils.getCachedValue
import com.explyt.util.ExplytKotlinUtil.filterToSet
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.findPsiFile
import com.intellij.psi.PsiElement
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.uast.*
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

private const val MAX_FILE_SIZE_BYTES = 1024 * 1024 //1mb

object EndpointUsageSearcher {

    fun findOpenApiJsonEndpoints(
        path: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        return findOpenApiJsonData(module.project).asSequence()
            .filter { it.psiFile is JsonFile }
            .filter { ModuleUtilCore.findModuleForFile(it.psiFile) == module }
            .flatMap { it.endpoints }
            .filterIsInstance<EndpointData.ReferrerData>()
            .map { it.referrer }
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapToList { it.psiElement }
    }

    fun findOpenApiJsonData(project: Project): List<EndpointFileData> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val result = FilenameIndex.getAllFilesByExt(project, "json", GlobalSearchScope.projectScope(project))
                .asSequence()
                .mapNotNull { it.findPsiFile(project) }
                .filterIsInstance<JsonFile>()
                .filter { it.virtualFile.length < MAX_FILE_SIZE_BYTES }
                .filter { OpenApiUtils.isOpenApi(it) }
                .map { EndpointFileData(it, collectOpenApiJsonEndpoints(it)) }
                .toList()
            CachedValueProvider.Result(result, modificationTracker(project))
        }
    }

    private fun collectOpenApiJsonEndpoints(file: JsonFile): List<EndpointData> {
        if (!OpenApiUtils.isOpenApi(file)) return emptyList()

        val topValue = file.topLevelValue as? JsonObject ?: return emptyList()
        val paths = topValue.findProperty(PATHS)?.value as? JsonObject ?: return emptyList()

        val endpoints = mutableListOf<EndpointData>()

        for (pathElement in paths.propertyList) {
            val path = SpringWebUtil.simplifyUrl(pathElement.name)

            val pathElementValue = pathElement.value as? JsonObject ?: continue

            val requestMethods = mutableListOf<String>()
            for (method in pathElementValue.propertyList) {
                val methodName = method.name
                if (methodName in REQUEST_METHODS) {
                    requestMethods.add(methodName.uppercase())
                    endpoints.add(EndpointData.ReferrerData(Referrer(path, methodName.uppercase(), method)))
                }
            }

            if (requestMethods.isNotEmpty()) {
                endpoints.add(
                    EndpointData.EndpointElementData(
                        EndpointElement(path, requestMethods, pathElement, null, file, EndpointType.OPENAPI)
                    )
                )
            }
        }

        return endpoints
    }

    fun findOpenApiYamlEndpoints(
        path: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        return findOpenApiYamlData(module.project).asSequence()
            .filter { it.psiFile is YAMLFile }
            .filter { ModuleUtilCore.findModuleForFile(it.psiFile) == module }
            .flatMap { it.endpoints }
            .filterIsInstance<EndpointData.ReferrerData>()
            .map { it.referrer }
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapToList { it.psiElement }
    }

    fun findOpenApiYamlData(project: Project): List<EndpointFileData> {
        return CachedValuesManager.getManager(project).getCachedValue(project) {
            val result = findOpenApiYamlFiles(project).asSequence()
                .mapNotNull { it.findPsiFile(project) }
                .filterIsInstance<YAMLFile>()
                .filter { OpenApiUtils.isOpenApi(it) }
                .map { EndpointFileData(it, collectOpenApiYamlEndpoints(it)) }
                .toList()
            CachedValueProvider.Result(result, modificationTracker(project))
        }
    }

    private fun findOpenApiYamlFiles(project: Project): List<VirtualFile> {
        return (FilenameIndex.getAllFilesByExt(
            project, "yaml",
            GlobalSearchScope.projectScope(project)
        ) + FilenameIndex.getAllFilesByExt(
            project, "yml",
            GlobalSearchScope.projectScope(project)
        ))
    }

    private fun modificationTracker(project: Project): ModificationTracker? {
        val modificationTracker = project.getService(OpenApiLanguagesModificationTracker::class.java)
            ?: ModificationTracker.NEVER_CHANGED
        return modificationTracker
    }

    private fun collectOpenApiYamlEndpoints(file: YAMLFile): List<EndpointData> {
        val endpoints = mutableListOf<EndpointData>()

        if (!OpenApiUtils.isOpenApi(file)) return emptyList()

        for (document in file.documents) {
            document.name
        }

        val paths = YAMLUtil.getTopLevelKeys(file)
            .firstOrNull { it.name == PATHS }
            ?.value as? YAMLMapping
            ?: return emptyList()

        for (pathElement in paths.keyValues) {
            val urlPath = pathElement.name ?: continue
            val path = SpringWebUtil.simplifyUrl(urlPath)

            val pathElementValue = pathElement.value as? YAMLMapping ?: continue

            val requestMethods = mutableListOf<String>()
            for (method in pathElementValue.keyValues) {
                val methodName = method.name ?: continue
                if (methodName in REQUEST_METHODS) {
                    requestMethods.add(methodName.uppercase())
                    endpoints.add(EndpointData.ReferrerData(Referrer(path, methodName.uppercase(), method)))
                }
            }

            if (requestMethods.isNotEmpty()) {
                endpoints.add(
                    EndpointData.EndpointElementData(
                        EndpointElement(path, requestMethods, pathElement, null, file, EndpointType.OPENAPI)
                    )
                )
            }
        }

        return endpoints
    }

    fun findMockMvcEndpointUsage(
        fullPath: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        val endpoint = SpringWebUtil.simplifyUrl(fullPath)
        val basePath = ApplicationBasePath.cachedOf(module)
        val methods = mutableSetOf<PsiElement>()

        for (psiMethod in getMockMvcMethods(module)) {
            if (!isInRequestMethods(psiMethod, requestMethods)) continue

            methods += SpringSearchUtils
                .searchReferenceByMethod(module, psiMethod, GlobalSearchScopesCore.projectTestScope(module.project))
                .asSequence()
                .mapNotNull { it.element.context.toUElementOfType<UCallExpression>() }
                .filterToSet { uCallExpression ->
                    val httpMethodIndex = SpringWebUtil.getHttpMethodIndex(psiMethod)
                    if (httpMethodIndex >= 0) {
                        if (uCallExpression
                                .getArgumentValueAsEnumName(httpMethodIndex) !in requestMethods
                        ) return@filterToSet false
                    }

                    val urlArg = uCallExpression.getArgumentForParameter(urlArgumentIndex(psiMethod))
                        ?.let(UrlArgumentText::of)
                        ?: return@filterToSet false

                    return@filterToSet EndpointUrlMatcher.addresses(
                        endpoint, urlArg, basePath, EndpointUrlMatcher.Policy.IN_PROCESS
                    )
                }
                .mapNotNull { it.sourcePsi }
        }

        return methods.toList()
    }

    /**
     * Every request a test sends to the endpoint at [fullPath]: through MockMvc, through `WebTestClient` and through
     * a real HTTP client to this machine.
     */
    fun findTestRequestUsage(fullPath: String, requestMethods: List<String>, module: Module): List<PsiElement> =
        (findMockMvcEndpointUsage(fullPath, requestMethods, module) +
                findWebTestClientEndpointUsage(fullPath, requestMethods, module) +
                findHttpClientEndpointUsage(fullPath, requestMethods, module)).distinct()

    /**
     * The requests a test sends to the endpoint at [fullPath] through a real HTTP client - `java.net.http`,
     * `RestTemplate`, `TestRestTemplate`, `RestClient` - to this machine; a request to another host is a call to
     * another service.
     */
    fun findHttpClientEndpointUsage(
        fullPath: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        val endpoint = SpringWebUtil.simplifyUrl(fullPath)
        val basePath = ApplicationBasePath.cachedOf(module)
        val testScope = GlobalSearchScopesCore.projectTestScope(module.project)

        return TestRequestReceivers.NETWORK.asSequence()
            .flatMap { receiver -> receiverMethods(receiver, module).map { receiver to it } }
            .filter { (_, method) -> TestRequestReceivers.urlParameterIndex(method) != -1 }
            .flatMap { (receiver, method) ->
                SpringSearchUtils.searchReferenceByMethod(module, method, testScope).asSequence()
                    .mapNotNull { it.element.context.toUElementOfType<UCallExpression>() }
                    .filter { call ->
                        val sent = TestRequestReceivers.httpMethodOf(call, method, receiver.verb)
                        if (requestMethods.isNotEmpty() && sent != null && sent !in requestMethods) return@filter false
                        val url = call.getArgumentForParameter(TestRequestReceivers.urlParameterIndex(method))
                            ?.let(UrlArgumentText::of)
                            ?: return@filter false
                        EndpointUrlMatcher.addresses(endpoint, url, basePath)
                    }
            }
            .mapNotNull { it.sourcePsi }
            .distinct()
            .toList()
    }

    private fun receiverMethods(receiver: TestRequestReceivers.Receiver, module: Module): List<PsiMethod> {
        val libraryTracker = ModificationTrackerManager.getInstance(module.project).getLibraryTracker()
        val methodsByOwner = getCachedValue(module, libraryTracker) { networkReceiverMethods(module) }
        return methodsByOwner[receiver.owner].orEmpty().filter { it.name == receiver.method }
    }

    private fun networkReceiverMethods(module: Module): Map<String, List<PsiMethod>> =
        TestRequestReceivers.NETWORK.map { it.owner }.distinct().associateWith { owner ->
            SpringCoreUtil.getClassMethodsFromLibraries(owner, module)?.toList()
                ?: JavaPsiFacade.getInstance(module.project)
                    .findClass(owner, module.getModuleWithDependenciesAndLibrariesScope(true))
                    ?.methods?.toList()
                ?: emptyList()
        }

    private fun isInRequestMethods(psiMethod: PsiMethod, requestMethods: List<String>): Boolean {
        val name = psiMethod.name
        if (name !in REQUEST_METHODS) return false

        val uppercaseName = name.uppercase()
        if (uppercaseName !in REQUEST_METHODS_WITH_TYPE) {
            if (uppercaseName !in requestMethods) return false
        }

        return urlArgumentIndex(psiMethod) != -1
    }

    /** The URL argument of a request builder: a URL template, or the `java.net.URI` of the overloads that take one. */
    private fun urlArgumentIndex(psiMethod: PsiMethod): Int =
        getUrlTemplateIndex(psiMethod).takeIf { it != -1 } ?: UrlArgumentText.uriParameterIndex(psiMethod)

    private fun getMockMvcMethods(module: Module): Array<PsiMethod> {
        val libraryModificationTracker = ModificationTrackerManager.getInstance(module.project).getLibraryTracker()

        return getCachedValue(module, libraryModificationTracker) {
            doGetMockMvcMethods(module)
        }
    }

    private fun doGetMockMvcMethods(module: Module): Array<PsiMethod> {
        return SpringCoreUtil.getClassMethodsFromLibraries(SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS, module)
            ?: emptyArray()
    }

    fun findWebTestClientEndpointUsage(path: String, requestedMethods: List<String>, module: Module): List<PsiElement> {
        return requestedMethods
            .flatMap { findWebTestClientEndpointUsage(path, it, module) }
    }

    fun findWebTestClientEndpointUsage(path: String, methodName: String, module: Module): List<PsiElement> {
        val endpoint = SpringWebUtil.simplifyUrl(path)
        val basePath = ApplicationBasePath.cachedOf(module)
        return getGetWebTestMethods(module).asSequence()
            .filter { it.name.uppercase() == methodName || it.name == "method" }
            .flatMap {
                SpringSearchUtils.searchReferenceByMethod(
                    module,
                    it.javaPsi,
                    GlobalSearchScopesCore.projectTestScope(module.project)
                )
            }
            .mapNotNull { it.element.parent?.toUElementOfType<UCallExpression>() }
            .filter {
                if (it.methodName != "method") return@filter true

                it.getArgumentValueAsEnumName(0) == methodName
            }
            .mapNotNull {
                it.uastParent?.uastParent as? UQualifiedReferenceExpression
            }
            .mapNotNull { it.selector as? UCallExpression }
            .filter { it.methodName == "uri" }
            .filter {
                val argument = it.valueArguments.firstOrNull()?.let(UrlArgumentText::of)
                    ?: return@filter false
                EndpointUrlMatcher.addresses(endpoint, argument, basePath)
            }
            .mapNotNull { it.sourcePsi }
            .toList()
    }

    private fun getGetWebTestMethods(module: Module): Collection<UMethod> {
        val uastModelAndLibraryTracker =
            ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()

        return getCachedValue(module, uastModelAndLibraryTracker) {
            doGetWebTestMethods(module)
        }
    }

    private fun doGetWebTestMethods(module: Module): Collection<UMethod> {
        return SpringCoreUtil.getClassMethodsFromLibraries(SpringWebClasses.WEB_TEST_CLIENT, module)
            ?.asSequence()
            ?.mapNotNull { it.toUElementOfType<UMethod>() }
            ?.filterToSet { it.name in REQUEST_METHODS }
            ?: return emptyList()
    }

}