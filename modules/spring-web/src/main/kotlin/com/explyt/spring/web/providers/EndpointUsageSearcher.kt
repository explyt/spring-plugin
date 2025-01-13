/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.web.providers

import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.core.util.UastUtil.getArgumentValueAsEnumName
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.editor.openapi.OpenApiUtils
import com.explyt.spring.web.loader.EndpointData
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.Referrer
import com.explyt.spring.web.tracker.OpenApiLanguagesModificationTracker
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.SpringWebUtil.PATHS
import com.explyt.spring.web.util.SpringWebUtil.REQUEST_METHODS
import com.explyt.spring.web.util.SpringWebUtil.REQUEST_METHODS_WITH_TYPE
import com.explyt.spring.web.util.SpringWebUtil.getUrlTemplateIndex
import com.explyt.util.CacheUtils.getCachedValue
import com.explyt.util.ExplytKotlinUtil.filterToSet
import com.explyt.util.ExplytKotlinUtil.mapToList
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import org.jetbrains.uast.*
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLMapping

object EndpointUsageSearcher {

    fun findOpenApiJsonEndpoints(
        path: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        return getOpenApiJsonEndpoints(module).asSequence()
            .filterIsInstance<EndpointData.ReferrerData>()
            .map { it.referrer }
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapToList { it.psiElement }
    }

    fun getOpenApiJsonEndpoints(module: Module): List<EndpointData> {
        return findOpenApiJsonFiles(module)
            .flatMap { collectOpenApiJsonEndpoints(it) }
    }

    fun findOpenApiJsonFiles(module: Module): List<JsonFile> {
        val project = module.project
        val psiManager = PsiManager.getInstance(project)

        return getCachedValue(module, modificationTracker(project)) {
            FilenameIndex.getAllFilesByExt(module.project, "json", GlobalSearchScope.moduleScope(module))
                .asSequence()
                .mapNotNull { psiManager.findFile(it) }
                .filterIsInstance<JsonFile>()
                .filter { OpenApiUtils.isOpenApi(it) }
                .toList()
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
        return getOpenApiYamlEndpoints(module).asSequence()
            .filterIsInstance<EndpointData.ReferrerData>()
            .map { it.referrer }
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapToList { it.psiElement }
    }

    fun getOpenApiYamlEndpoints(module: Module): List<EndpointData> {
        val project = module.project

        return getCachedValue(module, modificationTracker(project)) {
            findOpenApiYamlFiles(module)
                .flatMap { collectOpenApiYamlEndpoints(it) }
        }
    }

    fun findOpenApiYamlFiles(module: Module): List<YAMLFile> {
        val project = module.project
        val psiManager = PsiManager.getInstance(project)

        return getCachedValue(module, modificationTracker(project)) {
            (FilenameIndex.getAllFilesByExt(
                module.project, "yaml",
                GlobalSearchScope.moduleScope(module)
            ) + FilenameIndex.getAllFilesByExt(
                module.project, "yml",
                GlobalSearchScope.moduleScope(module)
            )).asSequence()
                .mapNotNull { psiManager.findFile(it) }
                .filterIsInstance<YAMLFile>()
                .filter { OpenApiUtils.isOpenApi(it) }
                .toList()
        }
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

                    val urlTemplateIndex = getUrlTemplateIndex(psiMethod)
                    val urlArg =
                        uCallExpression.getArgumentForParameter(urlTemplateIndex)?.evaluateString()
                            ?: return@filterToSet false

                    return@filterToSet SpringWebUtil.isEndpointMatches(endpoint, urlArg)
                }
                .mapNotNull { it.sourcePsi }
        }

        return methods.toList()
    }

    private fun isInRequestMethods(psiMethod: PsiMethod, requestMethods: List<String>): Boolean {
        val name = psiMethod.name
        if (name !in REQUEST_METHODS) return false

        val uppercaseName = name.uppercase()
        if (uppercaseName !in REQUEST_METHODS_WITH_TYPE) {
            if (uppercaseName !in requestMethods) return false
        }

        return getUrlTemplateIndex(psiMethod) != -1
    }

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
                val argument = it.valueArguments.firstOrNull()?.evaluate() as? String
                    ?: return@filter false
                SpringWebUtil.isEndpointMatches(endpoint, argument)
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