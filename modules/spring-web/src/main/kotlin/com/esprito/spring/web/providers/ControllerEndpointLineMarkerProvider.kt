package com.esprito.spring.web.providers

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.service.MetaAnnotationsHolder
import com.esprito.spring.core.service.SpringSearchService
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.spring.web.SpringWebBundle
import com.esprito.spring.web.SpringWebClasses
import com.esprito.spring.web.util.SpringWebUtil
import com.esprito.spring.web.util.SpringWebUtil.OPEN_API
import com.esprito.spring.web.util.SpringWebUtil.PATHS
import com.esprito.spring.web.util.SpringWebUtil.REQUEST_METHODS
import com.esprito.util.EspritoPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerInfo
import com.intellij.codeInsight.daemon.RelatedItemLineMarkerProvider
import com.intellij.codeInsight.navigation.NavigationGutterIconBuilder
import com.intellij.codeInsight.navigation.impl.PsiTargetPresentationRenderer
import com.intellij.icons.AllIcons
import com.intellij.json.psi.JsonElement
import com.intellij.json.psi.JsonFile
import com.intellij.json.psi.JsonObject
import com.intellij.openapi.editor.markup.GutterIconRenderer
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.NotNullLazyValue
import com.intellij.psi.*
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.PsiShortNamesCache
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.childrenOfType
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.getUParentForIdentifier
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLMapping
import javax.swing.Icon

class ControllerEndpointLineMarkerProvider : RelatedItemLineMarkerProvider() {

    override fun collectNavigationMarkers(
        element: PsiElement,
        result: MutableCollection<in RelatedItemLineMarkerInfo<*>>
    ) {
        val module = ModuleUtilCore.findModuleForPsiElement(element) ?: return
        val uParent = getUParentForIdentifier(element)

        if (uParent !is UMethod) return
        val psiMethod = uParent.javaPsi
        if (!psiMethod.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) return
        val psiClass = psiMethod.containingClass ?: return
        if (!psiClass.isMetaAnnotatedBy(SpringWebClasses.CONTROLLER)) return

        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)
        val path = requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("path", "value")).asSequence()
            .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
            .firstOrNull() ?: return

        val prefix = if (psiClass.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING)) {
            requestMappingMah.getAnnotationMemberValues(psiClass, setOf("path", "value")).asSequence()
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .firstOrNull() ?: ""
        } else {
            ""
        }

        val fullPath = SpringWebUtil.simplifyUrl("$prefix/$path")

        val requestMethods =
            requestMappingMah.getAnnotationMemberValues(psiMethod, setOf("method"))
                .map { it.text.split('.').last() }

        result += NavigationGutterIconBuilder.create(SpringIcons.ReadAccess)
            .setAlignment(GutterIconRenderer.Alignment.LEFT)
            .setTargets(NotNullLazyValue.lazy {
                findOpenApiJsonEndpoints(fullPath, requestMethods, module) +
                        findOpenApiYamlEndpoints(fullPath, requestMethods, module) +
                        findMockMvcEndpointUsage(fullPath, requestMethods, module)
            })
            .setTargetRenderer { getTargetRenderer() }
            .setTooltipText(SpringWebBundle.message("esprito.spring.web.gutter.endpoint.tooltip"))
            .setPopupTitle(SpringWebBundle.message("esprito.spring.web.gutter.endpoint.popup"))
            .setEmptyPopupText(SpringWebBundle.message("esprito.spring.web.gutter.endpoint.empty"))
            .createLineMarkerInfo(element)
    }

    private fun getTargetRenderer(): PsiTargetPresentationRenderer<PsiElement> {
        return object : PsiTargetPresentationRenderer<PsiElement>() {
            override fun getIcon(element: PsiElement): Icon? {
                if (element is YAMLKeyValue)
                    return AllIcons.FileTypes.Yaml
                if (element is JsonElement)
                    return AllIcons.FileTypes.Json

                return super.getIcon(element)
            }

            override fun getContainerText(element: PsiElement): String {
                return element.containingFile.name
            }
        }
    }

    private fun findOpenApiJsonEndpoints(
        path: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        return getOpenApiJsonEndpoints(module).asSequence()
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapTo(mutableListOf()) { it.psiElement }
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

    private fun findOpenApiYamlEndpoints(
        path: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        return getOpenApiYamlEndpoints(module).asSequence()
            .filter { it.path == path }
            .filter { it.method == null || requestMethods.contains(it.method) }
            .mapTo(mutableListOf()) { it.psiElement }
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
            .firstOrNull() { it.name == PATHS }
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

    private fun findMockMvcEndpointUsage(
        fullPath: String,
        requestMethods: List<String>,
        module: Module
    ): List<PsiElement> {
        val methods = getMockMvcMethods(module).asSequence()
            .filter { it.name.uppercase() in requestMethods }
            .filter { it.parameterList.getParameter(0)?.type?.canonicalText == "java.lang.String" }
            .flatMap {
                SpringSearchService.getInstance(module.project).getAllReferencesToElement(it)
            }
            .filter { it is PsiReferenceExpression }
            .map { it.element }
            .mapNotNull { it.context as? PsiMethodCallExpression }
            .filterTo(mutableSetOf()) {
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
                    ModificationTrackerManager.getInstance(module.project).getUastModelAndLibraryTracker()
                )
            }
    }

    private fun doGetMockMvcMethods(module: Module): Array<PsiMethod> {
        return PsiShortNamesCache.getInstance(module.project)
            .getClassesByName(
                "MockMvcRequestBuilders",
                GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
            )
            .firstOrNull() { it.qualifiedName == SpringWebClasses.MOCK_MVC_REQUEST_BUILDERS }
            ?.methods
            ?: emptyArray()
    }

    data class Referrer(val path: String, val method: String?, val psiElement: PsiElement)

}