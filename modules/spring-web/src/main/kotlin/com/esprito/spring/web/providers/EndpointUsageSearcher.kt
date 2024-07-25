package com.esprito.spring.web.providers

import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.core.util.SpringCoreUtil
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.providers.ControllerEndpointLineMarkerProvider.Referrer
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.spring.web.util.SpringWebUtil.OPEN_API
import com.esprito.spring.web.util.SpringWebUtil.PATHS
import com.esprito.spring.web.util.SpringWebUtil.REQUEST_METHODS
import com.esprito.util.EspritoKotlinUtil.filterToSet
import com.esprito.util.EspritoKotlinUtil.mapToList
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.module.Module
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.GlobalSearchScopesCore
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UQualifiedReferenceExpression
import org.jetbrains.uast.toUElementOfType
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
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapToList { it.psiElement }
    }

    private fun getOpenApiJsonEndpoints(module: Module): List<Referrer> {
        val psiManager = PsiManager.getInstance(module.project)

        return CachedValuesManager.getManager(module.project)
            .getCachedValue(module) {
                CachedValueProvider.Result(
                    FilenameIndex.getAllFilesByExt(module.project, "json", GlobalSearchScope.moduleScope(module))
                        .mapNotNull { psiManager.findFile(it) }
                        .filterIsInstance<JsonFile>()
                        .flatMap { collectOpenApiJsonEndpoints(it) },
                    ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
                )
            }
    }

    private fun collectOpenApiJsonEndpoints(file: JsonFile): List<Referrer> {
        val topValue = file.topLevelValue as? JsonObject ?: return emptyList()
        topValue.findProperty(OPEN_API)?.value ?: return emptyList()
        val paths = topValue.findProperty(PATHS)?.value as? JsonObject ?: return emptyList()

        val endpoints = mutableListOf<Referrer>()

        for (pathElement in paths.propertyList) {
            val path = SpringWebUtil.simplifyUrl(pathElement.name)
            endpoints.add(Referrer(path, null, pathElement))

            val pathElementValue = pathElement.value as? JsonObject ?: continue

            for (method in pathElementValue.propertyList) {
                val methodName = method.name
                if (methodName in REQUEST_METHODS) {
                    endpoints.add(Referrer(path, methodName.uppercase(), method))
                }
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
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapToList { it.psiElement }
    }

    private fun getOpenApiYamlEndpoints(module: Module): List<Referrer> {
        val psiManager = PsiManager.getInstance(module.project)

        return CachedValuesManager.getManager(module.project)
            .getCachedValue(module) {
                CachedValueProvider.Result(
                    (FilenameIndex.getAllFilesByExt(
                        module.project, "yaml",
                        GlobalSearchScope.moduleScope(module)
                    ) + FilenameIndex.getAllFilesByExt(
                        module.project, "yml",
                        GlobalSearchScope.moduleScope(module)
                    ))
                        .mapNotNull { psiManager.findFile(it) }
                        .filterIsInstance<YAMLFile>()
                        .flatMap { collectOpenApiYamlEndpoints(it) },
                    ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
                )
            }
    }

    private fun collectOpenApiYamlEndpoints(file: YAMLFile): List<Referrer> {
        val endpoints = mutableListOf<Referrer>()

        val topLevelKeys = YAMLUtil.getTopLevelKeys(file)
        if (topLevelKeys.none { YAMLUtil.getConfigFullName(it) == OPEN_API }) return emptyList()

        for (document in file.documents) {
            document.name
        }

        val paths = topLevelKeys
            .firstOrNull { it.name == PATHS }
            ?.value as? YAMLMapping
            ?: return emptyList()

        for (pathElement in paths.keyValues) {
            val urlPath = pathElement.name ?: continue
            val path = SpringWebUtil.simplifyUrl(urlPath)
            endpoints.add(Referrer(path, null, pathElement))

            val pathElementValue = pathElement.value as? YAMLMapping ?: continue

            for (method in pathElementValue.keyValues) {
                val methodName = method.name ?: continue
                if (methodName in REQUEST_METHODS) {
                    endpoints.add(Referrer(path, methodName.uppercase(), method))
                }
            }
        }

        return endpoints
    }

    fun findMockMvcEndpointUsage(
        fullPath: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        val methods = getMockMvcMethods(module).asSequence()
            .filter { it.name.uppercase() in requestMethods }
            .filter { it.parameterList.getParameter(0)?.type?.canonicalText == "java.lang.String" }
            .flatMap {
                SpringSearchService.getInstance(module.project)
                    .searchReferenceByMethod(module, it, GlobalSearchScopesCore.projectTestScope(module.project))
            }
            .filter { it is PsiReferenceExpression }
            .map { it.element }
            .mapNotNull { it.context as? PsiMethodCallExpression }
            .filterToSet {
                val firstArg = (it.childrenOfType<PsiExpressionList>()
                    .firstOrNull()?.expressions?.firstOrNull() as? PsiLiteralExpression)
                    ?.value as? String
                firstArg != null && SpringWebUtil.simplifyUrl(firstArg) == fullPath
            }

        return methods.toList()
    }

    private fun getMockMvcMethods(module: Module): Array<PsiMethod> {
        return CachedValuesManager.getManager(module.project)
            .getCachedValue(module) {
                CachedValueProvider.Result(
                    doGetMockMvcMethods(module),
                    ModificationTrackerManager.getInstance(module.project).getLibraryTracker()
                )
            }
    }

    private fun doGetMockMvcMethods(module: Module): Array<PsiMethod> {
        return SpringCoreUtil.getClassMethodsFromLibraries(SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS, module)
            ?: emptyArray()
    }

    fun findWebTestClientEndpointUsage(path: String, methodName: String, module: Module): List<PsiElement> {
        return getGetWebTestMethods(module).asSequence()
            .filter { it.name.uppercase() == methodName }
            .flatMap {
                SpringSearchService.getInstance(module.project)
                    .searchReferenceByMethod(
                        module,
                        it.javaPsi,
                        GlobalSearchScopesCore.projectTestScope(module.project)
                    )
            }
            .mapNotNull {
                it.element.parent?.parent?.parent
                    ?.toUElementOfType<UQualifiedReferenceExpression>()
            }
            .mapNotNull { it.selector as? UCallExpression }
            .filter { it.methodName == "uri" }
            .filter {
                val argument = it.valueArguments.firstOrNull()?.evaluate() as? String
                    ?: return@filter false
                SpringWebUtil.isMatchingTemplate(path, argument)
            }
            .mapNotNull { it.sourcePsi }
            .toList()
    }

    private fun getGetWebTestMethods(module: Module): Collection<UMethod> {
        return CachedValuesManager.getManager(module.project)
            .getCachedValue(module) {
                CachedValueProvider.Result(
                    doGetWebTestMethods(module),
                    ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
                )
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