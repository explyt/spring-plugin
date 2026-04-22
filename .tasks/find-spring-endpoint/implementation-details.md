# MCP Tool & Endpoint Searcher — Full Implementation Details

## 1. `SpringMcpProvider.kt` (lines 1–170)

**File:** `modules/spring-ai/src/main/kotlin/com/explyt/spring/ai/mcp/SpringMcpProvider.kt`

```kotlin
/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.util.SpringBootUtil
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.util.projectScope


class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("explyt_get_spring_boot_applications")
    @McpDescription(description = "Returns all SpringBootApplications - fully-qualified (e.g. 'java.util.List') Java class names in the project")
    suspend fun getAllSpringBootApplications(
        @McpDescription("Path to the project root")
        projectPath: String
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val applications = withContext(Dispatchers.IO) {
            readAction {
                //val project = coroutineContext.project
                val springBootAppAnnotations = PackageScanService.getInstance(project).getSpringBootAppAnnotations()
                springBootAppAnnotations.asSequence()
                    .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, project.projectScope()) }
                    .distinctBy { it.qualifiedName }
                    .toList()
                    .mapNotNull { toSpringBootApplicationDto(it) }
            }
        }

        return mapper.writeValueAsString(applications)
    }

    @McpTool("explyt_get_project_beans_by_spring_boot_application")
    @McpDescription(description = "Returns all project's Spring Beans in SpringBootApplication by bean type")
    suspend fun applicationBeans(
        @McpDescription("Fully-qualified class name for the SpringBootApplication")
        applicationClassName: String,
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription(
            "Filter results by a Bean Type. Possible values: " +
                    "ASPECT - for org.aspectj.lang.annotation.Aspect, \n" +
                    "MESSAGE_MAPPING - for KafkaListener/RabbitListener and other inheritor of org.springframework.messaging.handler.annotation.MessageMapping, \n" +
                    "CONTROLLER - for org.springframework.stereotype.Controller and inheritors , \n" +
                    "AUTO_CONFIGURATION - for org.springframework.boot.autoconfigure.AutoConfiguration and inheritors , \n" +
                    "CONFIGURATION_PROPERTIES - for org.springframework.boot.context.properties.ConfigurationProperties and inheritors , \n" +
                    "CONFIGURATION - for org.springframework.context.annotation.Configuration and inheritors , \n" +
                    "REPOSITORY - for Spring Data Repositories and org.springframework.stereotype.Repository , \n" +
                    "COMPONENT - for Spring Components/Service and other beans. \n"
        )
        beanType: String,
    ): String {
        val project = getCurrentProject(projectPath)
            ?: getCurrentProjectForClass(applicationClassName)
            ?: mcpFail("project not found")
        val mcpBeanType = getMcpBeanType(beanType) ?: mcpFail("bean type not found $beanType")
        val springBeans = withContext(Dispatchers.IO) {
            readAction {
                val applicationPsiClass = JavaPsiFacade.getInstance(project)
                    .findClass(applicationClassName, project.projectScope())
                    ?: mcpFail("Spring Boot Application class not found $applicationClassName")
                val module = ModuleUtilCore.findModuleForPsiElement(applicationPsiClass)
                    ?: mcpFail("Module not found for $applicationClassName")
                McpBeanSearchService.getInstance(project).getProjectBeansMcp(module)
            }
        }
        val beans = springBeans.asSequence()
            .filter { it.beanType == mcpBeanType }
            .map { McpSpringBean(it.beanName, it.className, it.moduleName) }
            .toList()
        return mapper.writeValueAsString(beans)
    }

    private fun toSpringBootApplicationDto(psiClass: PsiClass): SpringBootApplicationJson? {
        val qualifiedName = psiClass.qualifiedName ?: return null
        val springBootInfo = SpringBootUtil.getSpringBootStartersInfo(psiClass)
        return SpringBootApplicationJson(
            fullyQualifiedClassName = qualifiedName,
            springBootVersion = SpringBootUtil.getSpringBootVersion(psiClass),
            springBootStarters = springBootInfo?.second ?: emptyList(),
            moduleName = ModuleUtilCore.findModuleForPsiElement(psiClass)?.name,
            buildTool = springBootInfo?.first
        )
    }

    private fun getMcpBeanType(beanType: String): McpBeanTypes? {
        return try {
            McpBeanTypes.valueOf(beanType.uppercase())
        } catch (_: Exception) {
            null
        }
    }

    companion object {
        private val mapper = ObjectMapper()
    }
}

private fun getCurrentProject(projectPath: String?): Project? {
    projectPath ?: return null
    val openProjects = ProjectManager.getInstance().openProjects
        .filter { !it.isDefault }
    if (openProjects.size == 1) return openProjects[0]

    return openProjects.find { it.basePath == projectPath }
}

private suspend fun getCurrentProjectForClass(applicationClassName: String? = null): Project? {
    if (applicationClassName != null) {
        val openProjects = ProjectManager.getInstance().openProjects.filter { !it.isDefault }
        for (project in openProjects) {
            val applicationPsiClass = readAction {
                JavaPsiFacade.getInstance(project).findClass(applicationClassName, project.projectScope())
            }
            if (applicationPsiClass != null) return project
        }
    }
    return null
}

data class SpringBootApplicationJson(
    val fullyQualifiedClassName: String,
    val springBootVersion: String,
    val springBootStarters: List<String>,
    val moduleName: String?,
    val buildTool: String?,
)

data class SpringBootApplication(
    @param:McpDescription("fully-qualified java class name for Spring Boot Application Main") val className: String
)

data class McpSpringBean(
    @param:McpDescription("Spring Bean name") val beanName: String,
    @param:McpDescription("full qualified java class name for Spring Bean") val className: String,
    @param:McpDescription("project module name where Spring Bean located") val moduleName: String,
)
```

---

## 2. `SpringWebEndpointsSearcher.kt` (lines 1–102)

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/service/SpringWebEndpointsSearcher.kt`

```kotlin
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

package com.explyt.spring.web.service

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.WebEeClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.util.CacheUtils
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules

@Service(Service.Level.PROJECT)
class SpringWebEndpointsSearcher(private val project: Project) {
    companion object {
        fun getInstance(project: Project): SpringWebEndpointsSearcher = project.service()
    }

    fun getLoadersTypes(): Collection<EndpointType> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(project)
            .map { it.getType() }
            .distinct()
    }

    fun getAllEndpoints(module: Module, types: List<EndpointType> = emptyList()): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { types.isEmpty() || types.contains(it.getType()) }
            .filter { it.isApplicable(module) }
            .flatMapTo(mutableListOf()) { it.searchEndpoints(module) }
    }

    fun getAllEndpoints(): List<EndpointElement> {
        val distinctElements = mutableSetOf<EndpointElement>()

        return project.modules.flatMapTo(mutableListOf()) { module ->
            getAllEndpoints(module)
                .filter { endpoint: EndpointElement ->
                    val isUnique = !distinctElements.contains(endpoint)
                    distinctElements.add(endpoint)
                    isUnique
                }
        }
    }

    fun getAllEndpointElements(
        urlPath: String,
        module: Module,
        types: List<EndpointType> = emptyList()
    ): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project)
            .filter { types.isEmpty() || types.contains(it.getType()) }
            .flatMapTo(mutableListOf()) { it.getEndpointElements(urlPath, module) }
    }

    fun getJaxRsApplicationPath(module: Module): String {
        return CacheUtils.getCachedValue(
            module,
            ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
        ) {
            val applicationPathTargetClass = WebEeClasses.JAX_RS_APPLICATION_PATH.getTargetClass(module)
            val applicationPathMah = MetaAnnotationsHolder.of(module, applicationPathTargetClass)

            val httpAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
                module, applicationPathTargetClass, false
            ).takeIf { it.isNotEmpty() } ?: emptyList()

            httpAnnotations.asSequence()
                .flatMap {
                    SpringSearchService.getInstance(module.project)
                        .searchAnnotatedClasses(it, module)
                }
                .flatMap { applicationPathMah.getAnnotationMemberValues(it, setOf("value")) }
                .mapNotNull { AnnotationUtil.getStringAttributeValue(it) }
                .filter { it.isNotBlank() }
                .firstOrNull() ?: ""
        }
    }

}
```

---

## 3. `SpringWebEndpointsLoader.kt` (lines 1–94)

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/loader/SpringWebEndpointsLoader.kt`

```kotlin
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

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.web.util.SpringWebUtil
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.searches.AnnotatedElementsSearch

interface SpringWebEndpointsLoader {

    fun searchEndpoints(module: Module): List<EndpointElement>

    fun getType(): EndpointType

    fun getEndpointElements(urlPath: String, module: Module): List<EndpointElement> {
        if (!getType().isWeb) return emptyList()
        val searchUrl = SpringWebUtil.simplifyUrl(urlPath)

        return searchEndpoints(module)
            .filter { SpringWebUtil.isEndpointMatches(it.path, searchUrl) }
    }

    fun searchAnnotatedClasses(annotation: PsiClass, module: Module): List<PsiClass> =
        SpringSearchService.getInstance(module.project).searchAnnotatedClasses(annotation, module)

    fun searchAnnotatedMethods(annotation: PsiClass, module: Module): List<PsiMethod> {
        return AnnotatedElementsSearch.searchPsiMethods(annotation, module.moduleWithDependenciesScope).toList()
    }

    fun isApplicable(module: Module): Boolean

    companion object {
        val EP_NAME = ProjectExtensionPointName<SpringWebEndpointsLoader>(
            "com.explyt.spring.web.springWebEndpointsLoader"
        )

    }
}

data class Referrer(
    val path: String,
    val method: String?,
    val psiElement: PsiElement
)

data class EndpointElement(
    val path: String,
    val requestMethods: List<String>,
    val psiElement: PsiElement,
    val containingClass: PsiClass?,
    val containingFile: PsiFile?,
    val type: EndpointType
)

sealed class EndpointData {
    data class ReferrerData(val referrer: Referrer) : EndpointData()
    data class EndpointElementData(val endpointElement: EndpointElement) : EndpointData()
}

data class EndpointFileData(val psiFile: PsiFile, val endpoints: List<EndpointData>)

enum class EndpointType(val readable: String, val isWeb: Boolean) {
    SPRING_BOOT("Spring Boot", false),
    SPRING_MVC("Spring MVC", true),
    SPRING_HTTP_EXCHANGE("HttpExchange", true),
    SPRING_JAX_RS("JAX-RS", true),
    SPRING_WEBFLUX("WebFlux", true),
    OPENAPI("OpenAPI", false),
    SPRING_OPEN_FEIGN("OpenFeign", true),
    MESSAGE_BROKER("Message Broker", false),
    EVENT_LISTENERS("Event Listeners", false),
}
```

---

## 4. `SpringWebControllerLoader.kt` (lines 1–111, full file)

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/loader/SpringWebControllerLoader.kt`

```kotlin
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

package com.explyt.spring.web.loader

import com.explyt.spring.core.service.MetaAnnotationsHolder
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.AnnotationUtil
import com.intellij.codeInsight.MetaAnnotationUtil
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiClass
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager

class SpringWebControllerLoader(private val project: Project) : SpringWebEndpointsLoader {

    private val cachedValuesManager = CachedValuesManager.getManager(project)

    override fun isApplicable(module: Module) = SpringWebUtil.isWebModule(module)

    override fun searchEndpoints(module: Module): List<EndpointElement> {
        return cachedValuesManager.getCachedValue(module) {
            CachedValueProvider.Result(
                doSearchEndpoints(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun getType(): EndpointType {
        return EndpointType.SPRING_MVC
    }

    private fun doSearchEndpoints(module: Module): List<EndpointElement> {
        val controllerAnnotations = MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.CONTROLLER, false
        ).takeIf { it.isNotEmpty() } ?: return emptyList()

        val allAnnotations = controllerAnnotations + MetaAnnotationUtil.getAnnotationTypesWithChildren(
            module, SpringWebClasses.SWAGGER_API, false
        )
        val requestMappingMah = MetaAnnotationsHolder.of(module, SpringWebClasses.REQUEST_MAPPING)

        return allAnnotations.asSequence().flatMap { searchAnnotatedClasses(it, module) }
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
                        controllerPsiClass,
                        null,
                        EndpointType.SPRING_MVC
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
```

---

## 5. `mcp-server-plugin.xml` (lines 1–23)

**File:** `modules/spring-ai/src/main/resources/META-INF/mcp-server-plugin.xml`

```xml
<!--
  ~ Copyright © 2025 Explyt Ltd
  ~
  ~ All rights reserved.
  ~
  ~ This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
  ~
  ~ You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
  ~
  ~ By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
  ~ If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
  ~
  ~ You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
  ~
  ~ Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
  -->

<idea-plugin>
    <extensions defaultExtensionNs="com.intellij">
        <mcpServer.mcpToolset implementation="com.explyt.spring.ai.mcp.SpringBootApplicationMcpToolset"/>
    </extensions>
</idea-plugin>
```

---

## 6. `SpringWebUtil.kt` — `simplifyUrl` and `isEndpointMatches` methods

**File:** `modules/spring-web/src/main/kotlin/com/explyt/spring/web/util/SpringWebUtil.kt`

### `simplifyUrl` (lines 653–662)

```kotlin
    fun simplifyUrl(urlPath: String): String {
        var result = if (urlPath.startsWith("/")) urlPath else "/$urlPath"

        result = result.replace(MultipleSlashes, "/")

        if (result.endsWith("/") && result.length > 1) {
            result = result.substring(0, result.length - 1)
        }
        return result.split('?').first()
    }
```

### `isEndpointMatches` (lines 670–672)

```kotlin
    fun isEndpointMatches(endpoint: String, path: String): Boolean {
        return getRegexByUri(simplifyUrl(endpoint)).matches(simplifyUrl(path))
    }
```

### `getRegexByUri` (lines 678–685) — private helper

```kotlin
    private fun getRegexByUri(path: String): Regex {
        return endpointRegExByUri.computeIfAbsent(path) {
            val regex = path
                .replace(TEMPLATE_PARAM_REGEX, "[^/?]+")
                .replace(MULTIPLE_ASTERISKS, "*")
            Regex("^$regex(\\?.*)?$")
        }
    }
```

### Related constants (lines 695, 731–733)

```kotlin
    private val MultipleSlashes = Regex("//+")                              // line 695
    private val endpointRegExByUri = ConcurrentHashMap<String, Regex>()     // line 731
    private val TEMPLATE_PARAM_REGEX = Regex("\\{[^}]+}")                   // line 732
    private val MULTIPLE_ASTERISKS = Regex("\\*{2,}")                       // line 733
```
