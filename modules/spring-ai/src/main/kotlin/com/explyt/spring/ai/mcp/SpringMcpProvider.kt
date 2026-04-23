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

import com.explyt.jpa.JpaClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytAnnotationUtil.findFirstAnnotation
import com.explyt.util.ExplytAnnotationUtil.getBooleanAttribute
import com.explyt.util.ExplytAnnotationUtil.getMemberValues
import com.explyt.util.ExplytAnnotationUtil.getStringAttribute
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.mcpFail
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.toUElement


class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("explyt_get_spring_boot_applications")
    @McpDescription(description = "Returns all SpringBootApplications - fully-qualified (e.g. 'java.util.List') Java class names in the project")
    suspend fun getAllSpringBootApplications(
        @McpDescription("Path to the project root")
        projectPath: String
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val applications = withContext(Dispatchers.IO) {
            smartReadAction(project) {
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
            smartReadAction(project) {
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

    @McpTool("explyt_find_spring_endpoint")
    @McpDescription(
        description = "Finds endpoints matching a URL pattern across all endpoint types: " +
                "Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign, OpenAPI, message brokers (Kafka/RabbitMQ listeners), " +
                "and event listeners. " +
                "Resolves composed paths from class-level @RequestMapping and method-level @GetMapping/@PostMapping etc. " +
                "Returns matching endpoints with full path, HTTP methods, controller class, method name, parameters, " +
                "return type, file path, line number, and endpoint type. " +
                "Supports partial URL matching (e.g. 'requests' matches '/api/.../requests') " +
                "and path variable wildcards (e.g. '{id}' matches any path variable name)."
    )
    suspend fun findEndpoint(
        @McpDescription(
            "URL pattern to search for. Can be: " +
                    "a full path like '/api/orgs/{orgId}/drilldown/{metricId}/requests', " +
                    "a partial path like '/drilldown/requests' or just 'requests', " +
                    "or a path with wildcards like '/{id}/requests'. " +
                    "Path variables like {orgId} are treated as wildcards matching any segment."
        )
        urlPattern: String,
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription(
            "Optional HTTP method filter: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS. " +
                    "Leave empty to match all methods."
        )
        httpMethod: String = "",
    ): String {
        if (urlPattern.isBlank()) mcpFail("urlPattern must not be empty")
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val normalizedPattern = SpringWebUtil.simplifyUrl(urlPattern)
        val methodFilter = httpMethod.trim().uppercase().takeIf { it.isNotEmpty() }

        val endpoints = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val allEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()

                allEndpoints.asSequence()
                    .filter { matchesUrlPattern(it, normalizedPattern) }
                    .filter { methodFilter == null || it.requestMethods.isEmpty() || it.requestMethods.any { m -> m.equals(methodFilter, ignoreCase = true) } }
                    .take(MAX_ENDPOINT_RESULTS)
                    .map { toEndpointJson(it, project) }
                    .toList()
            }
        }

        return mapper.writeValueAsString(endpoints)
    }

    private fun matchesUrlPattern(endpoint: EndpointElement, normalizedPattern: String): Boolean {
        val endpointPath = SpringWebUtil.simplifyUrl(endpoint.path)
        // Exact/regex match (handles {param} wildcards)
        if (SpringWebUtil.isEndpointMatches(endpointPath, normalizedPattern)) return true
        // Substring match for partial URLs
        if (endpointPath.contains(normalizedPattern)) return true
        return false
    }

    private fun toEndpointJson(endpoint: EndpointElement, project: Project): EndpointJson {
        val psiMethod = endpoint.psiElement as? PsiMethod
        val controllerClass = endpoint.containingClass ?: psiMethod?.containingClass
        val basePath = project.basePath?.let { "$it/" }
        val filePath = (endpoint.containingFile ?: endpoint.psiElement.containingFile)?.virtualFile?.path
        val relativePath = if (basePath != null && filePath != null && filePath.startsWith(basePath)) {
            filePath.removePrefix(basePath)
        } else {
            filePath
        }
        val rawLine = endpoint.psiElement.getLineNumber(start = true)
        val lineNumber = if (rawLine >= 0) rawLine + 1 else 1

        val parameters = if (psiMethod != null) extractParameters(psiMethod) else emptyList()
        val returnType = psiMethod?.returnType?.canonicalText

        return EndpointJson(
            httpMethods = endpoint.requestMethods.ifEmpty { listOf("ALL") },
            fullPath = endpoint.path,
            controllerClass = controllerClass?.qualifiedName,
            methodName = psiMethod?.name,
            filePath = relativePath,
            line = lineNumber,
            parameters = parameters,
            returnType = returnType,
            endpointType = endpoint.type.readable,
        )
    }

    private fun extractParameters(psiMethod: PsiMethod): List<EndpointParameterJson> {
        val result = mutableListOf<EndpointParameterJson>()

        for (info in SpringWebUtil.collectPathVariables(psiMethod)) {
            result += EndpointParameterJson(info.name, "PATH", info.typeFqn, info.isRequired)
        }
        for (info in SpringWebUtil.collectRequestParameters(psiMethod)) {
            result += EndpointParameterJson(info.name, "QUERY", info.typeFqn, info.isRequired, info.defaultValue)
        }
        val body = SpringWebUtil.getRequestBodyInfo(psiMethod)
        if (body != null) {
            result += EndpointParameterJson(body.name, "BODY", body.typeFqn, body.isRequired)
        }
        for (info in SpringWebUtil.collectRequestHeaders(psiMethod)) {
            result += EndpointParameterJson(info.name, "HEADER", info.typeFqn, info.isRequired, info.defaultValue)
        }

        return result
    }

    // ---- explyt_get_spring_http_endpoints ----

    @McpTool("explyt_get_spring_http_endpoints")
    @McpDescription(
        description = "Lists all HTTP endpoints in the project. " +
                "Covers Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign, and Spring Boot actuator endpoints. " +
                "Each entry includes the HTTP method, full path, controller class, method name, return type, " +
                "file path, line number, and endpoint type. " +
                "Use optional filters to narrow results by controller class name or endpoint type."
    )
    suspend fun getHttpEndpoints(
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription("Optional substring filter on controller class name (e.g. 'Coverage'). Leave empty for all.")
        controllerFilter: String = "",
        @McpDescription(
            "Optional endpoint type filter. Possible values: SPRING_MVC, SPRING_WEBFLUX, SPRING_JAX_RS, " +
                    "SPRING_HTTP_EXCHANGE, SPRING_OPEN_FEIGN, SPRING_BOOT, OPENAPI. Leave empty for all."
        )
        endpointType: String = "",
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val controllerSubstring = controllerFilter.trim().takeIf { it.isNotEmpty() }
        val typeFilter = endpointType.trim().uppercase().takeIf { it.isNotEmpty() }

        val endpoints = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints().asSequence()
                    .filter { it.type.isWeb }
                    .filter { typeFilter == null || it.type.name.equals(typeFilter, ignoreCase = true) }
                    .filter {
                        if (controllerSubstring == null) true
                        else {
                            val cls = it.containingClass ?: (it.psiElement as? PsiMethod)?.containingClass
                            cls?.qualifiedName?.contains(controllerSubstring, ignoreCase = true) == true
                                    || cls?.name?.contains(controllerSubstring, ignoreCase = true) == true
                        }
                    }
                    .take(MAX_ENDPOINT_RESULTS)
                    .mapNotNull { toEndpointJson(it, project) }
                    .toList()
            }
        }

        return mapper.writeValueAsString(endpoints)
    }

    // ---- explyt_get_spring_endpoint_contract ----

    @McpTool("explyt_get_spring_endpoint_contract")
    @McpDescription(
        description = "Returns the full API contract for a specific endpoint. " +
                "Includes HTTP method, full path, all parameters (path variables, query params, request body, headers) " +
                "with types and required flags, return type, response DTO field schema (recursively expanded up to 3 levels), " +
                "produces/consumes media types, and the first service method called from the controller. " +
                "Use this after explyt_find_spring_endpoint or explyt_get_spring_http_endpoints to deeply inspect a single endpoint."
    )
    suspend fun getEndpointContract(
        @McpDescription(
            "URL pattern of the endpoint to inspect (e.g. '/api/orgs/{orgId}/project-success/v1/coverage/users'). " +
                    "Must match a single endpoint. If multiple match, all are returned."
        )
        urlPattern: String,
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription("Optional HTTP method filter: GET, POST, PUT, DELETE, etc. Leave empty for all.")
        httpMethod: String = "",
    ): String {
        if (urlPattern.isBlank()) mcpFail("urlPattern must not be empty")
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val normalizedPattern = SpringWebUtil.simplifyUrl(urlPattern)
        val methodFilter = httpMethod.trim().uppercase().takeIf { it.isNotEmpty() }

        val contracts = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val allEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()

                allEndpoints.asSequence()
                    .filter { matchesUrlPattern(it, normalizedPattern) }
                    .filter { methodFilter == null || it.requestMethods.isEmpty() || it.requestMethods.any { m -> m.equals(methodFilter, ignoreCase = true) } }
                    .take(MAX_ENDPOINT_RESULTS)
                    .mapNotNull { buildContract(it, project) }
                    .toList()
            }
        }

        return mapper.writeValueAsString(contracts)
    }

    private fun buildContract(endpoint: EndpointElement, project: Project): EndpointContractJson? {
        val psiMethod = endpoint.psiElement as? PsiMethod ?: return null
        val uMethod = psiMethod.toUElement() as? UMethod ?: return null
        val controllerClass = endpoint.containingClass ?: psiMethod.containingClass
        val parameters = extractParameters(psiMethod)
        val returnTypeFqn = psiMethod.returnType?.canonicalText

        // Response DTO schema
        val responseSchema = psiMethod.returnType?.let { expandType(it, project, depth = 3) }

        // Produces / consumes from @RequestMapping via UAST API
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod)
        var produces = emptyList<String>()
        var consumes = emptyList<String>()
        if (module != null) {
            val mah = SpringSearchService.getInstance(project).getMetaAnnotations(module, SpringWebClasses.REQUEST_MAPPING)
            produces = mah.getAnnotationValues(uMethod, setOf("produces"))
                .mapNotNull { it.evaluateString() }
            consumes = mah.getAnnotationValues(uMethod, setOf("consumes"))
                .mapNotNull { it.evaluateString() }
        }

        // First service call target
        val calledMethods = findCalledMethods(psiMethod)
        val serviceCall = calledMethods.firstOrNull()?.let { callee ->
            val calleeClass = callee.containingClass
            CallTargetJson(
                target = "${calleeClass?.qualifiedName ?: calleeClass?.name ?: "?"}.${callee.name}",
                line = lineOf(callee),
            )
        }

        return EndpointContractJson(
            httpMethods = endpoint.requestMethods.ifEmpty { listOf("ALL") },
            fullPath = endpoint.path,
            controllerClass = controllerClass?.qualifiedName,
            methodName = psiMethod.name,
            filePath = relativePathOf(psiMethod, project),
            line = lineOf(psiMethod),
            parameters = parameters,
            returnType = returnTypeFqn,
            responseSchema = responseSchema,
            produces = produces,
            consumes = consumes,
            serviceCall = serviceCall,
            endpointType = endpoint.type.readable,
        )
    }

    private fun expandType(psiType: PsiType, project: Project, depth: Int): DtoSchemaJson? {
        if (depth <= 0) return null
        val resolved = (psiType as? PsiClassType)?.resolve() ?: return null
        val fqn = resolved.qualifiedName ?: return null

        // Skip JDK / framework wrapper types — unwrap generics instead
        if (fqn.startsWith("java.") || fqn.startsWith("kotlin.") || fqn.startsWith("org.springframework.")) {
            // For generic wrappers (ResponseEntity<T>, List<T>, Optional<T>), expand the type argument
            val typeArgs = psiType.parameters
            if (typeArgs.isNotEmpty()) {
                return expandType(typeArgs[0], project, depth)
            }
            return null
        }

        val fields = mutableListOf<DtoFieldJson>()
        for (field in resolved.allFields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue
            val fieldType = field.type
            val nested = expandType(fieldType, project, depth - 1)
            fields += DtoFieldJson(
                name = field.name,
                type = fieldType.canonicalText,
                nullable = fieldType is PsiPrimitiveType && fieldType == PsiTypes.nullType()
                        || field.annotations.any { it.qualifiedName?.contains("Nullable") == true },
                nested = nested,
            )
        }

        // Also include Kotlin data class properties via getter methods (for classes without Java fields)
        if (fields.isEmpty()) {
            for (method in resolved.allMethods) {
                if (method.hasModifierProperty(PsiModifier.STATIC)) continue
                if (!method.name.startsWith("get") && !method.name.startsWith("is")) continue
                if (method.parameterList.parametersCount != 0) continue
                if (method.containingClass?.qualifiedName?.startsWith("java.") == true) continue
                val propName = method.name
                    .removePrefix("get").removePrefix("is")
                    .replaceFirstChar { it.lowercase() }
                val retType = method.returnType ?: continue
                val nested = expandType(retType, project, depth - 1)
                fields += DtoFieldJson(
                    name = propName,
                    type = retType.canonicalText,
                    nullable = false,
                    nested = nested,
                )
            }
        }

        return DtoSchemaJson(className = fqn, fields = fields)
    }

    @McpTool("explyt_trace_spring_call_chain")
    @McpDescription(
        description = "Traces the call chain from a given method through Spring layers (Controller → Service → Repository). " +
                "Returns the chain of methods with their Spring stereotype (CONTROLLER, SERVICE, REPOSITORY, COMPONENT, CONFIGURATION), " +
                "parameters, called methods, file paths, and line numbers. " +
                "Optionally finds test files that reference discovered methods. " +
                "Useful for understanding cross-cutting concerns and planning parameter-threading changes."
    )
    suspend fun traceCallChain(
        @McpDescription("Path to the source file containing the starting method (project-relative, e.g. 'src/main/kotlin/.../MyController.kt')")
        filePath: String,
        @McpDescription("1-based line number within the method to start tracing from")
        line: Int,
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription("How many layers deep to trace (default 3). Each layer follows method calls into injected beans.")
        depth: Int = 3,
        @McpDescription("Whether to find test files that reference the discovered methods (default true)")
        includeTests: Boolean = true,
    ): String {
        if (filePath.isBlank()) mcpFail("filePath must not be empty")
        if (line < 1) mcpFail("line must be >= 1")
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val effectiveDepth = depth.coerceIn(1, 10)

        val result = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val basePath = project.basePath ?: mcpFail("project base path not found")
                val absolutePath = "$basePath/$filePath"
                val virtualFile = LocalFileSystem.getInstance().findFileByPath(absolutePath)
                    ?: mcpFail("file not found: $filePath")
                val psiFile = PsiManager.getInstance(project).findFile(virtualFile)
                    ?: mcpFail("cannot parse file: $filePath")
                val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)
                    ?: mcpFail("cannot get document for: $filePath")

                val offset = document.getLineStartOffset((line - 1).coerceIn(0, document.lineCount - 1))
                val elementAtLine = psiFile.findElementAt(offset)
                // Use UAST to support both Java (PsiMethod) and Kotlin (KtNamedFunction)
                val psiMethod = elementAtLine
                    ?.getUastParentOfType<UMethod>()
                    ?.javaPsi
                    ?: mcpFail("no method found at $filePath:$line")

                val module = ModuleUtilCore.findModuleForPsiElement(psiMethod)
                val visited = mutableSetOf<PsiMethod>()
                val chain = buildChain(psiMethod, effectiveDepth, visited, project)

                val testReferences = if (includeTests && module != null) {
                    findTestReferences(visited, module, project)
                } else {
                    emptyList()
                }

                CallChainResultJson(chain = chain, testReferences = testReferences)
            }
        }

        return mapper.writeValueAsString(result)
    }

    private fun buildChain(
        psiMethod: PsiMethod,
        remainingDepth: Int,
        visited: MutableSet<PsiMethod>,
        project: Project,
    ): List<CallChainNodeJson> {
        if (remainingDepth <= 0 || !visited.add(psiMethod)) return emptyList()

        val containingClass = psiMethod.containingClass
        val calledMethods = findCalledMethods(psiMethod)

        val callsInto = calledMethods.map { callee ->
            CallTargetJson(
                target = "${callee.containingClass?.name ?: "?"}.${callee.name}",
                line = lineOf(callee),
            )
        }

        val node = CallChainNodeJson(
            layer = containingClass?.let { detectSpringLayer(it) },
            className = containingClass?.qualifiedName ?: containingClass?.name,
            methodName = psiMethod.name,
            filePath = relativePathOf(psiMethod, project),
            line = lineOf(psiMethod),
            parameters = psiMethod.parameterList.parameters.map { it.name },
            callsInto = callsInto,
        )

        val childNodes = calledMethods.flatMap { callee ->
            buildChain(callee, remainingDepth - 1, visited, project)
        }

        return listOf(node) + childNodes
    }

    private fun findCalledMethods(psiMethod: PsiMethod): List<PsiMethod> {
        val uMethod = psiMethod.toUElement() as? UMethod ?: return emptyList()
        val calls = mutableListOf<UCallExpression>()
        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                calls += node
                return false // keep descending so nested calls are discovered too
            }
        })
        return calls.asSequence()
            .mapNotNull { it.resolve() }
            .filter { it != psiMethod } // skip self-recursion
            .distinctBy { (it.containingClass?.qualifiedName ?: "") + "#" + it.name + "#" + it.parameterList.parametersCount }
            .toList()
    }

    private fun findTestReferences(
        methods: Set<PsiMethod>,
        module: com.intellij.openapi.module.Module,
        project: Project,
    ): List<TestReferenceJson> {
        val testScope = module.moduleTestsWithDependentsScope
        val byFile = mutableMapOf<String, MutableMap<String, MutableList<Int>>>()

        for (method in methods) {
            val refs = MethodReferencesSearch.search(method, testScope, true).findAll()
            for (ref in refs) {
                val refElement = ref.element
                val refFile = relativePathOf(refElement, project) ?: continue
                val refLine = lineOf(refElement)
                val methodKey = "${method.containingClass?.name ?: "?"}.${method.name}"
                byFile.getOrPut(refFile) { mutableMapOf() }
                    .getOrPut(methodKey) { mutableListOf() }
                    .add(refLine)
            }
        }

        return byFile.map { (file, methods) ->
            TestReferenceJson(
                filePath = file,
                referencedMethods = methods.map { (method, lines) ->
                    TestMethodReferenceJson(method = method, lines = lines.distinct().sorted())
                },
            )
        }
    }

    // ---- explyt_get_spring_data_entities ----

    @McpTool("explyt_get_spring_data_entities")
    @McpDescription(
        description = "Returns all JPA entities (@Entity annotated classes) in the project with their schema metadata. " +
                "Detects both javax.persistence and jakarta.persistence annotations. " +
                "Each entity includes class name, file path and line, table name (from @Table or default), " +
                "fields with column names, types, primary key flag, nullability, " +
                "and JPA relationships (@OneToOne, @OneToMany, @ManyToOne, @ManyToMany) with joinColumn/mappedBy metadata. " +
                "Also returns table indexes from @Table(indexes=[...]). " +
                "Use this for schema understanding, migration planning, DTO design, and query writing."
    )
    suspend fun getSpringDataEntities(
        @McpDescription("Path to the project root")
        projectPath: String,
        @McpDescription("Optional fully-qualified package prefix to restrict results (e.g. 'com.example.domain'). Leave empty for all packages.")
        packageFilter: String = "",
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail("project not found")
        val packagePrefix = packageFilter.trim().takeIf { it.isNotEmpty() }

        val entities = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val projectScope = project.projectScope()
                val librariesScope = GlobalSearchScope.allScope(project)
                val javaPsiFacade = JavaPsiFacade.getInstance(project)
                val entityClasses = ENTITY_ANNOTATION_FQNS.asSequence()
                    .mapNotNull { javaPsiFacade.findClass(it, librariesScope) }
                    .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, projectScope).findAll().asSequence() }
                    .distinctBy { it.qualifiedName }
                    .filter { cls ->
                        packagePrefix == null || cls.qualifiedName?.startsWith(packagePrefix) == true
                    }
                    .take(MAX_ENTITY_RESULTS)
                    .toList()

                entityClasses.mapNotNull { toEntityJson(it, project) }
            }
        }

        return mapper.writeValueAsString(entities)
    }

    private fun toEntityJson(psiClass: PsiClass, project: Project): SpringDataEntityJson? {
        val qualifiedName = psiClass.qualifiedName ?: return null
        val simpleName = psiClass.name ?: qualifiedName.substringAfterLast('.')
        val tableName = resolveTableName(psiClass, simpleName)
        val fields = collectEntityFields(psiClass)
        val indexes = collectEntityIndexes(psiClass)

        return SpringDataEntityJson(
            name = simpleName,
            className = qualifiedName,
            filePath = relativePathOf(psiClass, project),
            line = lineOf(psiClass),
            tableName = tableName,
            fields = fields,
            indexes = indexes,
        )
    }

    private fun resolveTableName(psiClass: PsiClass, defaultName: String): String {
        val tableName = psiClass.findFirstAnnotation(TABLE_ANNOTATION_FQNS).getStringAttribute(ATTR_NAME)
        if (tableName != null) return tableName
        val entityName = psiClass.findFirstAnnotation(ENTITY_ANNOTATION_FQNS).getStringAttribute(ATTR_NAME)
        return entityName ?: defaultName
    }

    private fun collectEntityFields(psiClass: PsiClass): List<EntityFieldJson> {
        val seen = mutableSetOf<String>()
        val result = mutableListOf<EntityFieldJson>()
        for (field in psiClass.allFields) {
            if (field.hasModifierProperty(PsiModifier.STATIC)) continue
            if (field.hasModifierProperty(PsiModifier.TRANSIENT)) continue
            if (field.findFirstAnnotation(TRANSIENT_ANNOTATION_FQNS) != null) continue
            if (!seen.add(field.name)) continue

            result += toEntityField(field)
        }
        return result
    }

    private fun toEntityField(field: PsiField): EntityFieldJson {
        val columnAnnotation = field.findFirstAnnotation(COLUMN_ANNOTATION_FQNS)
        val column = columnAnnotation.getStringAttribute(ATTR_NAME)
        val columnNullable = columnAnnotation.getBooleanAttribute(ATTR_NULLABLE)
        val relationshipMatch = RELATIONSHIP_ANNOTATIONS.firstNotNullOfOrNull { (fqns, kind) ->
            field.findFirstAnnotation(fqns)?.let { it to kind }
        }
        val relationshipAnnotation = relationshipMatch?.first
        val relationshipType = relationshipMatch?.second
        val joinColumn = field.findFirstAnnotation(JOIN_COLUMN_ANNOTATION_FQNS).getStringAttribute(ATTR_NAME)
        val mappedBy = relationshipAnnotation.getStringAttribute(ATTR_MAPPED_BY)
        val primaryKey = field.findFirstAnnotation(ID_ANNOTATION_FQNS) != null
        val nullable = columnNullable ?: !hasNotNullAnnotation(field)

        return EntityFieldJson(
            name = field.name,
            type = field.type.canonicalText,
            column = column,
            primaryKey = primaryKey,
            nullable = nullable,
            relationship = relationshipType,
            joinColumn = joinColumn,
            mappedBy = mappedBy,
        )
    }

    private fun collectEntityIndexes(psiClass: PsiClass): List<EntityIndexJson> {
        val tableAnnotation = psiClass.findFirstAnnotation(TABLE_ANNOTATION_FQNS) ?: return emptyList()
        return tableAnnotation.getMemberValues(ATTR_INDEXES)
            .filterIsInstance<PsiAnnotation>()
            .map { indexAnnotation ->
                EntityIndexJson(
                    name = indexAnnotation.getStringAttribute(ATTR_NAME),
                    columns = indexAnnotation.getStringAttribute(ATTR_COLUMN_LIST)
                        ?.split(',')
                        ?.map { it.trim() }
                        ?.filter { it.isNotBlank() }
                        ?: emptyList(),
                    unique = indexAnnotation.getBooleanAttribute(ATTR_UNIQUE) ?: false,
                )
            }
    }

    private fun hasNotNullAnnotation(field: PsiField): Boolean {
        return field.annotations.any { it.qualifiedName?.substringAfterLast('.') == NOT_NULL_SIMPLE_NAME }
    }

    private fun detectSpringLayer(psiClass: PsiClass): String? = when {
        psiClass.isMetaAnnotatedBy(SpringWebClasses.REST_CONTROLLER) -> "CONTROLLER"
        psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONTROLLER) -> "CONTROLLER"
        psiClass.isMetaAnnotatedBy(SpringCoreClasses.SERVICE) -> "SERVICE"
        psiClass.isMetaAnnotatedBy(SpringCoreClasses.REPOSITORY) -> "REPOSITORY"
        psiClass.isMetaAnnotatedBy(SpringCoreClasses.CONFIGURATION) -> "CONFIGURATION"
        psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT) -> "COMPONENT"
        else -> null
    }

    private fun relativePathOf(element: PsiElement, project: Project): String? {
        val basePath = project.basePath?.let { "$it/" } ?: return null
        val filePath = element.containingFile?.virtualFile?.path ?: return null
        return if (filePath.startsWith(basePath)) filePath.removePrefix(basePath) else filePath
    }

    private fun lineOf(element: PsiElement): Int {
        val raw = element.getLineNumber(start = true)
        return if (raw >= 0) raw + 1 else 1
    }

    companion object {
        private const val MAX_ENDPOINT_RESULTS = 50
        private const val MAX_ENTITY_RESULTS = 500
        private val mapper = ObjectMapper()

        private const val ATTR_NAME = "name"
        private const val ATTR_NULLABLE = "nullable"
        private const val ATTR_MAPPED_BY = "mappedBy"
        private const val ATTR_INDEXES = "indexes"
        private const val ATTR_COLUMN_LIST = "columnList"
        private const val ATTR_UNIQUE = "unique"
        private const val NOT_NULL_SIMPLE_NAME = "NotNull"

        private val ENTITY_ANNOTATION_FQNS = JpaClasses.entity.allFqns
        private val TABLE_ANNOTATION_FQNS = JpaClasses.table.allFqns
        private val COLUMN_ANNOTATION_FQNS = JpaClasses.column.allFqns
        private val ID_ANNOTATION_FQNS = JpaClasses.id.allFqns + JpaClasses.embeddedId.allFqns
        private val TRANSIENT_ANNOTATION_FQNS = JpaClasses.transient.allFqns
        private val JOIN_COLUMN_ANNOTATION_FQNS = JpaClasses.joinColumn.allFqns
        private val RELATIONSHIP_ANNOTATIONS: List<Pair<List<String>, String>> = listOf(
            JpaClasses.oneToOne.allFqns to "ONE_TO_ONE",
            JpaClasses.oneToMany.allFqns to "ONE_TO_MANY",
            JpaClasses.manyToOne.allFqns to "MANY_TO_ONE",
            JpaClasses.manyToMany.allFqns to "MANY_TO_MANY",
        )
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

data class EndpointJson(
    val httpMethods: List<String>,
    val fullPath: String,
    val controllerClass: String?,
    val methodName: String?,
    val filePath: String?,
    val line: Int,
    val parameters: List<EndpointParameterJson>,
    val returnType: String?,
    val endpointType: String,
)

data class EndpointParameterJson(
    val name: String,
    val source: String,
    val type: String,
    val required: Boolean,
    val defaultValue: String? = null,
)

data class CallChainResultJson(
    val chain: List<CallChainNodeJson>,
    val testReferences: List<TestReferenceJson>,
)

data class CallChainNodeJson(
    val layer: String?,
    val className: String?,
    val methodName: String,
    val filePath: String?,
    val line: Int,
    val parameters: List<String>,
    val callsInto: List<CallTargetJson>,
)

data class CallTargetJson(
    val target: String,
    val line: Int,
)

data class TestReferenceJson(
    val filePath: String,
    val referencedMethods: List<TestMethodReferenceJson>,
)

data class TestMethodReferenceJson(
    val method: String,
    val lines: List<Int>,
)

data class EndpointContractJson(
    val httpMethods: List<String>,
    val fullPath: String,
    val controllerClass: String?,
    val methodName: String?,
    val filePath: String?,
    val line: Int,
    val parameters: List<EndpointParameterJson>,
    val returnType: String?,
    val responseSchema: DtoSchemaJson?,
    val produces: List<String>,
    val consumes: List<String>,
    val serviceCall: CallTargetJson?,
    val endpointType: String,
)

data class DtoSchemaJson(
    val className: String,
    val fields: List<DtoFieldJson>,
)

data class DtoFieldJson(
    val name: String,
    val type: String,
    val nullable: Boolean,
    val nested: DtoSchemaJson?,
)

data class SpringDataEntityJson(
    val name: String,
    val className: String,
    val filePath: String?,
    val line: Int,
    val tableName: String,
    val fields: List<EntityFieldJson>,
    val indexes: List<EntityIndexJson>,
)

data class EntityFieldJson(
    val name: String,
    val type: String,
    val column: String?,
    val primaryKey: Boolean,
    val nullable: Boolean,
    val relationship: String?,
    val joinColumn: String?,
    val mappedBy: String?,
)

data class EntityIndexJson(
    val name: String?,
    val columns: List<String>,
    val unique: Boolean,
)

