/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.jpa.JpaClasses
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.providers.SpringBeanLineMarkerProvider
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.EndpointPathPatterns
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.util.ExplytAnnotationUtil.findFirstAnnotation
import com.explyt.util.ExplytAnnotationUtil.getBooleanAttribute
import com.explyt.util.ExplytAnnotationUtil.getMemberValues
import com.explyt.util.ExplytAnnotationUtil.getStringAttribute
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.codeInspection.isInheritorOf
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.psi.*
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.psi.util.InheritanceUtil
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting
import org.jetbrains.kotlin.idea.base.psi.getLineNumber
import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtCallableDeclaration
import org.jetbrains.kotlin.psi.KtNullableType
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.toUElement

private const val MCP_EXPECTED_ERROR_CLASS = "com.intellij.mcpserver.McpExpectedError"

private fun mcpFail(message: String): Nothing = throw createMcpExpectedError(message)

private fun createMcpExpectedError(message: String): Throwable {
    return runCatching {
        Class.forName(MCP_EXPECTED_ERROR_CLASS)
            .getConstructor(String::class.java)
            .newInstance(message) as Throwable
    }.getOrElse { IllegalArgumentException(message) }
}

/**
 * What every tool of this family says about its project argument.
 *
 * One text for all of them: a caller who learns the rule from one tool can rely on it for the rest.
 */
private const val PROJECT_PATH_DESCRIPTION =
    "Path to the project root. Omit it when a single project is open; when several are, it is required, " +
            "and a path naming none of them is refused rather than answered from another one."

class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("explyt_get_spring_boot_applications")
    @McpDescription(
        description = "Call first when entering a Spring workspace you do not know, and before any " +
                "explyt_get_project_beans_by_spring_boot_application call, which needs one of these class names. " +
                "A multi-module workspace can hold several @SpringBootApplication classes, and guessing the wrong one " +
                "scopes every later bean question to the wrong module. " +
                "Returns each application's fully-qualified class name, Spring Boot version, starters, module and " +
                "build tool - the version and starters answer 'which Boot line and which features are on the " +
                "classpath' without opening the build file."
    )
    suspend fun getAllSpringBootApplications(
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
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
    @McpDescription(
        description = "Call before adding a component, to see which beans of that stereotype already exist and " +
                "what they are named; before injecting by type, to check that exactly one candidate exists; and " +
                "when asked what a module contributes to the context. " +
                "Lists the beans of one Spring Boot application, filtered by stereotype, from the IDE's bean " +
                "model: it includes @Bean factory methods, meta-annotated stereotypes and @Import-ed configurations, " +
                "which a text search for '@Service' or '@Component' never finds. " +
                "Returns each bean's name, fully-qualified class and module, one row per name a bean answers to. " +
                "An empty 'moduleName' means the bean has no module in this project - a library bean, or one a " +
                "loaded context reports without project sources; the bean is still listed. " +
                "By default the answer comes from a loaded application context when one is available and from the " +
                "static model otherwise, where it is an estimate of the module rather than of a running context; " +
                "pass source=STATIC or source=NATIVE to choose, and contextId to name one of several loaded " +
                "contexts. For one bean by type or name, or for a single injection point, use " +
                "explyt_find_spring_bean instead. " +
                "Take applicationClassName from explyt_get_spring_boot_applications."
    )
    suspend fun applicationBeans(
        @McpDescription("Fully-qualified class name for the SpringBootApplication")
        applicationClassName: String,
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
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
        @McpDescription(
            "Which bean model answers: AUTO (default) prefers a loaded application context and falls back to " +
                    "the static model, STATIC always uses the static model, NATIVE requires a loaded context."
        )
        source: String = "AUTO",
        @McpDescription("Id of the loaded context to answer from, when several are loaded for this application.")
        contextId: String? = null,
    ): String {
        val project = getCurrentProject(projectPath)
            ?: projectPath?.takeIf { it.isNotBlank() }?.let { mcpFail(projectProblem(it)) }
            ?: getCurrentProjectForClass(applicationClassName)
            ?: mcpFail(projectProblem(projectPath))
        val mcpBeanType = getMcpBeanType(beanType) ?: mcpFail("bean type not found $beanType")
        val preference = getBeanSourcePreference(source) ?: mcpFail("unknown source $source")
        val springBeans = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val applicationPsiClass = JavaPsiFacade.getInstance(project)
                    .findClass(applicationClassName, project.projectScope())
                    ?: mcpFail("Spring Boot Application class not found $applicationClassName")
                try {
                    McpBeanSearchService.getInstance(project)
                        .getProjectBeansMcp(applicationPsiClass, preference, contextId)
                } catch (e: BeanQueryException) {
                    // A context that cannot be chosen is not an application without beans: an empty array would
                    // read as the latter, so the caller is told what to disambiguate instead.
                    mcpFail("${e.problem.code}: ${e.problem.message}")
                }
            }
        }
        val beans = springBeans.asSequence()
            .filter { it.beanType == mcpBeanType }
            .map { McpSpringBean(it.beanName, it.className, it.moduleName) }
            .toList()
        return mapper.writeValueAsString(beans)
    }

    private fun getBeanSourcePreference(source: String): BeanSourcePreference? =
        BeanSourcePreference.entries.firstOrNull { it.name.equals(source.trim(), ignoreCase = true) }

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
        description = "Call when a task starts from a URL - a bug report, a frontend call, a log line, a curl - " +
                "and right after adding or changing a mapping, to confirm the composed path and the bound parameters " +
                "registered as intended. " +
                "Resolves the URL against the IDE's endpoint model, not the source text: the path is composed from " +
                "the class-level @RequestMapping and the method-level @GetMapping/@PostMapping, so it never appears " +
                "as one string and a text search for it finds nothing, and a class-level prefix can silently give a " +
                "new method a different URL than the one written on it. " +
                "Covers Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign, OpenAPI, message brokers " +
                "(Kafka/RabbitMQ listeners) and event listeners. " +
                "Returns an object with 'totalCount' (how many endpoints matched), 'truncated' (true when more " +
                "matched than were returned), 'endpoints' and 'nearestByPrefix'. Each endpoint carries full path, " +
                "HTTP methods, controller class, method name, parameters with their binding source, return type, " +
                "file path, line and endpoint type. 'endpoints' lists the closest match to the pattern first: an " +
                "exact path, then one matching it as a pattern, then one merely containing it, and within each " +
                "group the way Spring picks a handler - literal before '{template}', fewer wildcards first - so " +
                "when several match one URL the first is the one that dispatches. " +
                "Covers annotation-declared handlers and functional routes alike; for a functional route the " +
                "controller class and method name are the bean factory that registers it, and 'parameters' holds " +
                "the path variables its URL template declares. " +
                "Matching is forgiving: 'requests' matches '/api/.../requests', and '{id}' matches any path variable. " +
                "When 'endpoints' is empty, no such route exists yet, and 'nearestByPrefix' lists the existing routes " +
                "that share the longest leading path with the pattern - the controller and the conventions a new " +
                "route has to fit; 'sharedPrefix' names that common path."
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
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription(
            "Optional HTTP method filter: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS. " +
                    "Leave empty to match all methods."
        )
        httpMethod: String = "",
    ): String {
        val result = lookupEndpoints(urlPattern, projectPath, httpMethod) { endpoint, project ->
            toEndpointJson(endpoint, project)
        }
        return mapper.writeValueAsString(result)
    }

    /**
     * The single-URL lookup shared by the find and contract tools.
     *
     * Matches are ordered by how closely they answer the pattern ([matchRank]) and then by
     * [EndpointPathPatterns.SPECIFICITY], so when a literal route and a `{template}` route both match the
     * pattern, the first element is the one Spring dispatches to. A pattern that matches nothing is not a dead
     * end: [EndpointLookupJson.nearestByPrefix] carries the routes sharing the longest leading path with it,
     * which is where a route that does not exist yet would be added.
     */
    private suspend fun <T> lookupEndpoints(
        urlPattern: String,
        projectPath: String?,
        httpMethod: String,
        toJson: (EndpointElement, Project) -> T,
    ): EndpointLookupJson<T> {
        if (urlPattern.isBlank()) mcpFail("urlPattern must not be empty")
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
        val normalizedPattern = SpringWebUtil.simplifyUrl(urlPattern)
        val methodFilter = httpMethod.trim().uppercase().takeIf { it.isNotEmpty() }

        return withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val allEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()

                val matching = allEndpoints
                    .mapNotNull { endpoint -> matchRank(endpoint, normalizedPattern)?.let { endpoint to it } }
                    .filter { (endpoint, _) -> methodFilter == null || endpoint.requestMethods.isEmpty() || endpoint.requestMethods.any { m -> m.equals(methodFilter, ignoreCase = true) } }
                    .sortedWith(
                        compareBy<Pair<EndpointElement, Int>> { it.second }
                            .thenBy(EndpointPathPatterns.SPECIFICITY) { SpringWebUtil.simplifyUrl(it.first.path) }
                    )
                    .map { it.first }
                // Every counted endpoint is converted: `toJson` returns a value for any endpoint element, so a
                // caller reading `totalCount` against `endpoints.size` cannot see them disagree while
                // `truncated` is false.
                val page = matching.asSequence()
                    .take(MAX_ENDPOINT_RESULTS)
                    .onEach { ProgressManager.checkCanceled() }
                    .map { toJson(it, project) }
                    .toList()

                // The neighbourhood of a miss is context, not an answer, so the method filter does not apply to
                // it: a POST sibling under the same prefix still names the controller a new GET route belongs to.
                val nearest = if (matching.isEmpty()) nearestByPrefix(allEndpoints, normalizedPattern, project) else null

                EndpointLookupJson(
                    totalCount = matching.size,
                    truncated = matching.size > MAX_ENDPOINT_RESULTS,
                    endpoints = page,
                    sharedPrefix = nearest?.first,
                    nearestByPrefix = nearest?.second ?: emptyList(),
                )
            }
        }
    }

    /**
     * The routes sharing the longest leading path with [normalizedPattern], as compact endpoints, with that path.
     *
     * Nothing is returned when the longest shared path is empty: every route in the project "shares" the root,
     * and listing all of them would say nothing about where the pattern belongs.
     */
    private fun nearestByPrefix(
        endpoints: List<EndpointElement>,
        normalizedPattern: String,
        project: Project,
    ): Pair<String, List<CompactEndpointJson>>? {
        val bySharedSegments = endpoints
            .groupBy { EndpointPathPatterns.sharedLeadingSegments(SpringWebUtil.simplifyUrl(it.path), normalizedPattern) }
        val (sharedSegments, nearest) = bySharedSegments.maxByOrNull { it.key } ?: return null
        if (sharedSegments == 0) return null

        val compact = nearest
            .sortedWith(compareBy(EndpointPathPatterns.SPECIFICITY) { SpringWebUtil.simplifyUrl(it.path) })
            .take(MAX_NEAREST_ROUTES)
            .onEach { ProgressManager.checkCanceled() }
            .map { toCompactEndpointJson(it, project) }
        return EndpointPathPatterns.prefixOf(normalizedPattern, sharedSegments) to compact
    }

    /**
     * How closely [endpoint] answers [normalizedPattern], lowest first, or `null` when it does not answer it.
     *
     * Matching is deliberately forgiving - a bare `items` finds `/api/demo/items` - so a long unrelated path that
     * merely contains the pattern matches too. Ordering those by path specificity alone puts such a path *above*
     * the route matching the pattern exactly, because specificity breaks ties by descending length. The caller
     * reads its answer off the first element, so how well an endpoint fits the query has to outrank how Spring
     * would dispatch between the endpoints that fit it equally well.
     */
    private fun matchRank(endpoint: EndpointElement, normalizedPattern: String): Int? {
        val endpointPath = SpringWebUtil.simplifyUrl(endpoint.path)
        return when {
            endpointPath == normalizedPattern -> EXACT_MATCH
            SpringWebUtil.isEndpointMatches(endpointPath, normalizedPattern) -> PATTERN_MATCH
            endpointPath.contains(normalizedPattern) -> SUBSTRING_MATCH
            else -> null
        }
    }

    private fun toCompactEndpointJson(endpoint: EndpointElement, project: Project): CompactEndpointJson {
        val declaringMethod = declaringMethodOf(endpoint)
        val controllerClass = endpoint.containingClass ?: declaringMethod?.containingClass
        val position = sourcePositionOf(endpoint.psiElement, project)
        // A loader may know the declaring file of an endpoint whose element has no source position of its own.
        val filePath = position.filePath
            ?: endpoint.containingFile?.let { relativePathOf(it, project) }

        return CompactEndpointJson(
            httpMethods = endpoint.requestMethods.ifEmpty { listOf("ALL") },
            fullPath = endpoint.path,
            controllerClass = controllerClass?.qualifiedName,
            methodName = declaringMethod?.name,
            filePath = filePath,
            line = position.line,
            endpointType = endpoint.type.readable,
        )
    }

    private fun toEndpointJson(endpoint: EndpointElement, project: Project): EndpointJson {
        val handler = requestHandlerOf(endpoint)
        val core = toCompactEndpointJson(endpoint, project)

        return EndpointJson(
            httpMethods = core.httpMethods,
            fullPath = core.fullPath,
            controllerClass = core.controllerClass,
            methodName = core.methodName,
            filePath = core.filePath,
            line = core.line,
            parameters = handler?.let { extractParameters(it) } ?: pathTemplateParameters(endpoint.path),
            returnType = handler?.returnType?.canonicalText,
            endpointType = core.endpointType,
        )
    }

    /**
     * The method whose declaration carries [endpoint], handler or not.
     *
     * A functional route is a call inside a `@Bean` factory rather than a member of its own, so the element the
     * loader reports is an expression. Naming the enclosing factory is what makes such a route reachable: its path
     * appears nowhere else in the project, and without the method a caller is left with a file and a line.
     */
    private fun declaringMethodOf(endpoint: EndpointElement): PsiMethod? =
        endpoint.psiElement as? PsiMethod
            ?: endpoint.psiElement.toUElement()?.getParentOfType<UMethod>()?.javaPsi

    /**
     * The method Spring invokes for [endpoint], or `null` when the endpoint has none to read a signature from.
     *
     * A functional route is registered by a `@Bean` factory returning a `RouterFunction`, and that factory's
     * signature describes the *registration*: reading parameters off it reports the injected handler bean as a
     * request parameter and `RouterFunction<ServerResponse>` as the response type. Both are invented facts about
     * the wire format, and a caller generating a client cannot tell them from real ones - the reason this is
     * rejected rather than reported with a caveat.
     */
    private fun requestHandlerOf(endpoint: EndpointElement): PsiMethod? =
        (endpoint.psiElement as? PsiMethod)?.takeUnless { it.returnType.isRouteRegistration() }

    private fun PsiType?.isRouteRegistration(): Boolean =
        this != null && ROUTE_FUNCTION_TYPES.any { isInheritorOf(it) }

    /**
     * The parameters the URL template itself declares, for an endpoint with no handler signature to read.
     *
     * The type is the wire type of a path segment rather than a target type: nothing has been found that converts
     * it, so naming anything narrower than `String` would state more than is known.
     */
    private fun pathTemplateParameters(path: String): List<EndpointParameterJson> =
        SpringWebUtil.NameInBracketsRx.findAll(path)
            .mapNotNull { it.groups[TEMPLATE_NAME_GROUP]?.value?.substringBefore(':')?.takeIf(String::isNotBlank) }
            .distinct()
            .map { EndpointParameterJson(it, "PATH", CommonClassNames.JAVA_LANG_STRING, required = true) }
            .toList()

    private fun extractParameters(psiMethod: PsiMethod): List<EndpointParameterJson> {
        val result = mutableListOf<EndpointParameterJson>()

        for (info in SpringWebUtil.collectPathVariables(psiMethod)) {
            result += EndpointParameterJson(info.name, "PATH", info.typeFqn, info.isRequired)
        }
        val multipartRequestNames = psiMethod.parameterList.parameters.asSequence()
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_PARAM) && isMultipartPart(it.type) }
            .map(::wireNameOf)
            .toSet()
        val servletMvc = isConfirmedServletMvc(psiMethod)
        for (info in SpringWebUtil.collectRequestParameters(psiMethod)) {
            val source = if (servletMvc && info.name in multipartRequestNames) "PART" else "QUERY"
            result += EndpointParameterJson(info.name, source, info.typeFqn, info.isRequired, info.defaultValue)
        }
        for (param in psiMethod.parameterList.parameters) {
            val part = param.findFirstAnnotation(listOf(SpringWebClasses.REQUEST_PART)) ?: continue
            result += EndpointParameterJson(
                name = wireNameOf(param),
                source = "PART",
                type = param.type.canonicalText,
                required = part.getBooleanAttribute("required") ?: true,
            )
        }
        val body = SpringWebUtil.getRequestBodyInfo(psiMethod)
        if (body != null) {
            result += EndpointParameterJson(body.name, "BODY", body.typeFqn, body.isRequired)
        }
        for (info in SpringWebUtil.collectRequestHeaders(psiMethod)) {
            result += EndpointParameterJson(info.name, "HEADER", info.typeFqn, info.isRequired, info.defaultValue)
        }

        result += parametersOutsideCollectors(psiMethod)
        return result
    }

    private fun isConfirmedServletMvc(psiMethod: PsiMethod): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false
        val scope = module.moduleWithLibrariesScope
        val facade = JavaPsiFacade.getInstance(psiMethod.project)
        val hasServletMvc = facade.findClass(SpringWebClasses.MVC_DISPATCHER_SERVLET, scope) != null
        val hasWebFlux = facade.findClass(SpringWebClasses.WEBFLUX_DISPATCHER_HANDLER, scope) != null
        return hasServletMvc && !hasWebFlux
    }

    private fun isMultipartPart(type: PsiType): Boolean = when (type) {
        is PsiArrayType -> isMultipartPart(type.componentType)
        is PsiClassType -> {
            val className = type.resolve()?.qualifiedName ?: type.canonicalText
            className in MULTIPART_PART_TYPES ||
                    InheritanceUtil.isInheritor(type, "java.util.Collection")
                    && type.parameters.any(::isMultipartPart)
        }
        else -> false
    }

    /**
     * The handler parameters none of the annotation collectors claims.
     *
     * Enumerating only annotated parameters drops everything bound by a `HandlerMethodArgumentResolver`, and a
     * dropped parameter is indistinguishable from one that was never declared. That is the dangerous half: a
     * `currentUser` argument absent from the output can mean either "the endpoint authenticates its caller" or
     * "the endpoint has no authorization at all", and those have opposite meanings for anyone reading the
     * contract to audit it. Reporting the parameter with a source of `UNKNOWN` says "there is an argument here I
     * cannot classify", which is actionable; silence is not.
     */
    private fun parametersOutsideCollectors(psiMethod: PsiMethod): List<EndpointParameterJson> =
        psiMethod.parameterList.parameters
            .filter { param -> COLLECTED_BINDING_ANNOTATIONS.none { param.isMetaAnnotatedBy(it) } }
            .map { param ->
                EndpointParameterJson(
                    name = wireNameOf(param),
                    source = sourceOfUncollected(param),
                    type = param.type.canonicalText,
                    // Not null-by-omission: the collected annotations declare requiredness, whereas a
                    // resolver's contract is private to the resolver. Defaulting to `true` or `false` here would
                    // invent a fact about the wire format, which is the failure this method exists to avoid.
                    required = null,
                )
            }

    /**
     * The name the client sends, which is not always the Java name: `@CookieValue("sid") String sessionId` is
     * `sid` on the wire. The collectors above already report the annotation's name, so reporting the
     * declared identifier here instead would make one parameter list speak two different vocabularies — and the
     * wire name is the one a generated client or a test fixture has to use.
     *
     * Falls back to the declared name, which is also Spring's own rule when the annotation names nothing.
     */
    private fun wireNameOf(param: PsiParameter): String {
        val annotation = param.findFirstAnnotation(NAMED_BINDING_ANNOTATIONS) ?: return param.name
        return annotation.getStringAttribute("value")
            ?: annotation.getStringAttribute(ATTR_NAME)
            ?: param.name
    }

    private fun sourceOfUncollected(param: PsiParameter): String = when {
        param.isMetaAnnotatedBy(SpringWebClasses.COOKIE_VALUE) -> "COOKIE"
        param.isMetaAnnotatedBy(SpringWebClasses.MODEL_ATTRIBUTE) -> "MODEL"
        FRAMEWORK_SUPPLIED_TYPES.any { InheritanceUtil.isInheritor(param.type, it) } -> "FRAMEWORK"
        else -> "UNKNOWN"
    }

    // ---- explyt_get_spring_http_endpoints ----

    @McpTool("explyt_get_spring_http_endpoints")
    @McpDescription(
        description = "Lists the HTTP endpoints of one controller (controllerFilter) or of the whole project. " +
                "Call before extending an existing controller: the listing shows its class-level prefix, its sibling " +
                "routes and the parameter conventions a new route must match, and a literal route that would " +
                "compete with a '{template}' sibling. " +
                "Call with compact=true for a first inventory of an API surface you do not know yet. " +
                "Covers Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign, and Spring Boot actuator endpoints. " +
                "Returns an object with 'totalCount' (how many endpoints matched the filters), 'offset' (the index " +
                "the returned page starts at), 'truncated' (true when more matches remain after this page), and " +
                "'endpoints'. One endpoint looks exactly like this - note 'httpMethods' is an array, and the keys " +
                "are 'fullPath' and 'line', not 'path' or 'lineNumber': " +
                "{\"httpMethods\":[\"GET\"],\"fullPath\":\"/api/demo/items/{id}\"," +
                "\"controllerClass\":\"com.example.app.web.DemoController\",\"methodName\":\"getItem\"," +
                "\"filePath\":\"src/main/java/com/example/app/web/DemoController.java\",\"line\":40," +
                "\"parameters\":[{\"name\":\"id\",\"source\":\"PATH\",\"type\":\"java.lang.Long\"," +
                "\"required\":true,\"defaultValue\":null}],\"returnType\":\"com.example.app.dto.DemoDto\"," +
                "\"endpointType\":\"SPRING_MVC\"}. " +
                "Pass compact=true to omit 'parameters' and 'returnType' entirely - they dominate the response, " +
                "and on a large project the full form can exceed 100 KB on a single line. " +
                "When 'truncated' is true, either narrow the result with the controller or endpoint-type filters, " +
                "or request the next page with 'offset' = 'offset' + number of returned endpoints."
    )
    suspend fun getHttpEndpoints(
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription("Optional substring filter on controller class name (e.g. 'Coverage'). Leave empty for all.")
        controllerFilter: String = "",
        @McpDescription(
            "Optional endpoint type filter. Possible values: SPRING_MVC, SPRING_WEBFLUX, SPRING_JAX_RS, " +
                    "SPRING_HTTP_EXCHANGE, SPRING_OPEN_FEIGN, SPRING_BOOT, OPENAPI. Leave empty for all."
        )
        endpointType: String = "",
        @McpDescription("Index of the first endpoint to return, for paging through large projects. Defaults to 0.")
        offset: Int = 0,
        @McpDescription(
            "Maximum number of endpoints to return. Defaults to $DEFAULT_ENDPOINT_PAGE_SIZE, " +
                    "capped at $MAX_ENDPOINT_LIST_RESULTS."
        )
        limit: Int = DEFAULT_ENDPOINT_PAGE_SIZE,
        @McpDescription(
            "Omit 'parameters' and 'returnType' from each endpoint. Use this for a first inventory of a project " +
                    "you do not know yet: those two fields dominate the response, and the controller/type " +
                    "filters cannot narrow it before you know which controllers exist. Defaults to false."
        )
        compact: Boolean = false,
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
        val controllerSubstring = controllerFilter.trim().takeIf { it.isNotEmpty() }
        val typeFilter = endpointType.trim().uppercase().takeIf { it.isNotEmpty() }
        val pageStart = offset.coerceAtLeast(0)
        val pageSize = limit.coerceIn(1, MAX_ENDPOINT_LIST_RESULTS)

        val result = withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val matching = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints().asSequence()
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
                    .toList()

                // Convert only the requested page: building an EndpointJson resolves PSI (module, line number,
                // parameters), which is far too expensive to do for every endpoint of a large project.
                // 'compact' also skips the parameter extraction itself, so it is cheaper to compute and not
                // merely smaller to send.
                val page = matching.asSequence()
                    .drop(pageStart)
                    .take(pageSize)
                    .onEach { ProgressManager.checkCanceled() }
                val endpoints: List<Any> = if (compact) {
                    page.map { toCompactEndpointJson(it, project) }.toList()
                } else {
                    page.map { toEndpointJson(it, project) }.toList()
                }

                EndpointListJson(
                    totalCount = matching.size,
                    offset = pageStart,
                    truncated = pageStart + endpoints.size < matching.size,
                    endpoints = endpoints,
                )
            }
        }

        return mapper.writeValueAsString(result)
    }

    // ---- explyt_get_spring_endpoint_contract ----

    @McpTool("explyt_get_spring_endpoint_contract")
    @McpDescription(
        description = "Call before writing a client, a test, a frontend call or an OpenAPI description against one " +
                "endpoint, and before changing its request or response shape, to see what callers currently depend " +
                "on. " +
                "Returns the full API contract of the endpoint: HTTP method, full path, every declared handler " +
                "parameter with its type, return type, response DTO field schema (recursively expanded up to " +
                "3 levels), produces/consumes media types, and the first resolved call on an injected Spring bean " +
                "(null when no such call can be confirmed). Reading the handler signature by hand misses what Spring " +
                "binds implicitly and what the DTO's nested " +
                "types serialise to. " +
                "Each parameter carries a 'source': PATH, QUERY, PART, BODY, HEADER, COOKIE or MODEL for an " +
                "annotation-bound one (whose 'name' is the wire name, not the Java name, and whose 'required' is " +
                "declared); FRAMEWORK for one the container supplies, such as WebRequest or Principal; and " +
                "UNKNOWN for one bound by a custom HandlerMethodArgumentResolver, whose wire format this tool " +
                "cannot read - inspect the source before treating an UNKNOWN parameter as absent. " +
                "'required' is null whenever nothing declares it. " +
                "'contractStatus' is COMPLETE when a handler method declares the endpoint and PARTIAL for a " +
                "functional route (coRouter/router/RouterFunctions.route) or an OpenAPI declaration, which have no " +
                "such signature: a PARTIAL contract still names the path, the verb, the declaring bean factory, " +
                "the handler function as 'serviceCall' and the path variables the URL template declares, and " +
                "'contractUnavailableReason' says what has to be read at the source - an empty 'parameters' there " +
                "means 'not declared here', never 'the endpoint takes nothing'. " +
                "Returns the same object shape as explyt_find_spring_endpoint - 'totalCount', 'truncated', " +
                "'endpoints' with the closest match to the pattern first, and 'nearestByPrefix' with " +
                "'sharedPrefix' when nothing matched - with a contract in place of each endpoint; every counted " +
                "endpoint is returned, so endpoints.size equals totalCount unless truncated. " +
                "Take the urlPattern from explyt_find_spring_endpoint or explyt_get_spring_http_endpoints."
    )
    suspend fun getEndpointContract(
        @McpDescription(
            "URL pattern of the endpoint to inspect (e.g. '/api/orgs/{orgId}/project-success/v1/coverage/users'). " +
                    "Should match a single endpoint. If several match, all are returned, the dispatching one first."
        )
        urlPattern: String,
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription("Optional HTTP method filter: GET, POST, PUT, DELETE, etc. Leave empty for all.")
        httpMethod: String = "",
    ): String {
        val result = lookupEndpoints(urlPattern, projectPath, httpMethod) { endpoint, project ->
            buildContract(endpoint, project)
        }
        return mapper.writeValueAsString(result)
    }

    /**
     * The contract of [endpoint], complete when a request-handling method declares it and partial otherwise.
     *
     * A partial contract is deliberately still a contract: dropping the endpoint instead would leave the caller
     * with a `totalCount` naming an endpoint it cannot see, which reads as "the route does not exist" - the
     * opposite of what the endpoint model found. What is knowable without a handler signature - the path, the
     * verb, the declaring bean factory, and the parameters the URL template itself declares - is reported, and
     * [EndpointContractJson.contractUnavailableReason] names what is missing rather than leaving the caller to
     * infer it from empty fields.
     */
    private fun buildContract(endpoint: EndpointElement, project: Project): EndpointContractJson {
        val handler = requestHandlerOf(endpoint)
        val uHandler = handler?.toUElement() as? UMethod
        val module = ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement)
        val mediaTypes = mediaTypesOf(uHandler, module, project)
        val core = toCompactEndpointJson(endpoint, project)

        val serviceCall = when {
            handler != null && uHandler != null && module != null ->
                findServiceCall(handler, uHandler, module, project)
            else -> routeHandlerCall(endpoint, project)
        }

        return EndpointContractJson(
            httpMethods = core.httpMethods,
            fullPath = core.fullPath,
            controllerClass = core.controllerClass,
            methodName = core.methodName,
            filePath = core.filePath,
            line = core.line,
            parameters = handler?.let { extractParameters(it) } ?: pathTemplateParameters(endpoint.path),
            returnType = handler?.returnType?.canonicalText,
            responseSchema = handler?.returnType?.let { expandType(it, project, depth = 3) },
            produces = mediaTypes.produces,
            consumes = mediaTypes.consumes,
            serviceCall = serviceCall,
            endpointType = endpoint.type.readable,
            contractStatus = if (handler != null) COMPLETE_CONTRACT else PARTIAL_CONTRACT,
            contractUnavailableReason = if (handler != null) null else contractUnavailableReason(endpoint),
        )
    }

    private data class MediaTypes(val produces: List<String>, val consumes: List<String>)

    private fun mediaTypesOf(
        uHandler: UMethod?,
        module: com.intellij.openapi.module.Module?,
        project: Project,
    ): MediaTypes {
        if (uHandler == null || module == null) return MediaTypes(emptyList(), emptyList())
        val mah = SpringSearchService.getInstance(project)
            .getMetaAnnotations(module, SpringWebClasses.REQUEST_MAPPING)
        return MediaTypes(
            produces = mah.getAnnotationValues(uHandler, setOf("produces")).mapNotNull { it.evaluateString() },
            consumes = mah.getAnnotationValues(uHandler, setOf("consumes")).mapNotNull { it.evaluateString() },
        )
    }

    /**
     * What a caller has to read itself, in the terms of the endpoint kind rather than as a generic failure.
     *
     * "No contract" and "the request shape lives somewhere this tool does not read" call for different next
     * steps, and a caller that cannot tell them apart treats both as "the endpoint takes nothing".
     */
    private fun contractUnavailableReason(endpoint: EndpointElement): String = when (endpoint.type) {
        EndpointType.OPENAPI ->
            "The endpoint is declared in an OpenAPI document; its request and response schema live in the " +
                    "specification, not in a handler signature"

        else ->
            "The route is registered functionally, so its request and response shape is read from ServerRequest " +
                    "inside the handler function rather than declared by a handler signature"
    }

    /**
     * The handler function a functional route delegates to, as the endpoint's service call.
     *
     * `handler::handle` in `GET(path, handler::handle)` is the one thing a functional route states about its own
     * behaviour, and it is where the request shape is actually read. Reporting it as `serviceCall` keeps the
     * partial contract navigable instead of ending at the registration.
     */
    private fun routeHandlerCall(endpoint: EndpointElement, project: Project): ServiceCallJson? {
        val route = endpoint.psiElement.toUElement()?.getParentOfType<UCallExpression>(strict = false) ?: return null
        val callee = route.valueArguments
            .firstNotNullOfOrNull { (it as? UCallableReferenceExpression)?.resolve() as? PsiMethod }
            ?: return null
        val position = sourcePositionOf(callee, project)
        return ServiceCallJson(
            target = "${callee.containingClass?.qualifiedName}.${callee.name}",
            filePath = position.filePath,
            line = position.line,
        )
    }

    private fun findServiceCall(
        psiMethod: PsiMethod,
        uMethod: UMethod,
        module: com.intellij.openapi.module.Module,
        project: Project,
    ): ServiceCallJson? {
        val controllerClass = psiMethod.containingClass ?: return null
        val beanClasses = SpringSearchService.getInstance(project).getProjectBeans(module).map { it.psiClass }
        val beanFields = controllerClass.allFields.filter { field ->
            if (!isInjectedBeanField(field)) return@filter false
            val fieldClass = (field.type as? PsiClassType)?.resolve() ?: return@filter false
            beanClasses.any { InheritanceUtil.isInheritorOrSelf(it, fieldClass, true) }
        }.toSet()
        if (beanFields.isEmpty()) return null

        var serviceMethod: PsiMethod? = null
        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                if (serviceMethod != null) return true
                val receiver = (node.receiver as? UResolvable)?.resolve()
                val field = when (receiver) {
                    is PsiField -> receiver.takeIf { it in beanFields }
                    is PsiParameter -> {
                        val constructor = receiver.declarationScope as? PsiMethod
                        beanFields.firstOrNull { it.name == receiver.name
                                && it.type == receiver.type
                                && constructor?.isConstructor == true
                                && constructor.containingClass == it.containingClass }
                    }
                    else -> null
                } ?: return false
                val callee = node.resolve() ?: return false
                val receiverClass = (field.type as? PsiClassType)?.resolve() ?: return false
                val calleeClass = callee.containingClass ?: return false
                val calleeFqn = calleeClass.qualifiedName ?: return false
                if (!callee.hasModifierProperty(PsiModifier.STATIC)
                    && !calleeFqn.startsWith("java.")
                    && !calleeFqn.startsWith("kotlin.")
                    && !calleeFqn.startsWith("org.springframework.")
                    && InheritanceUtil.isInheritorOrSelf(receiverClass, calleeClass, true)
                ) {
                    serviceMethod = callee
                    return true
                }
                return false
            }
        })
        val callee = serviceMethod ?: return null
        val position = sourcePositionOf(callee, project)
        return ServiceCallJson(
            target = "${callee.containingClass?.qualifiedName}.${callee.name}",
            filePath = position.filePath,
            line = position.line,
        )
    }

    private fun isInjectedBeanField(field: PsiField): Boolean {
        if (SpringBeanLineMarkerProvider.isAutowiredFieldExpression(field)) return true
        if (field.initializer != null) return false
        val constructors = field.containingClass?.constructors ?: return false
        if ((field as? KtLightField)?.kotlinOrigin is KtParameter) {
            return constructors.any { constructor ->
                constructor.parameterList.parameters.any { it.name == field.name && it.type == field.type }
            }
        }
        return constructors.any { constructor ->
            constructor.body?.statements?.any { statement ->
                val assignment = (statement as? PsiExpressionStatement)?.expression as? PsiAssignmentExpression
                assignment != null &&
                        (assignment.lExpression as? PsiReferenceExpression)?.resolve() == field &&
                        (assignment.rExpression as? PsiReferenceExpression)?.resolve() in constructor.parameterList.parameters
            } == true
        }
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
                type = renderDtoType(fieldType, (field as? KtLightField)?.kotlinOrigin as? KtCallableDeclaration),
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
                    type = renderDtoType(retType, (method as? KtLightMethod)?.kotlinOrigin as? KtCallableDeclaration),
                    nullable = false,
                    nested = nested,
                )
            }
        }

        return DtoSchemaJson(className = fqn, fields = fields)
    }

    private fun renderDtoType(type: PsiType, origin: KtCallableDeclaration?): String =
        renderDtoType(type, origin?.typeReference)

    private fun renderDtoType(type: PsiType, source: KtTypeReference?): String {
        val element = source?.typeElement ?: return type.canonicalText
        val typeArguments = (type as? PsiClassType)?.parameters.orEmpty()
        val sourceArguments = element.typeArgumentsAsTypes
        val name = if (typeArguments.isNotEmpty() && typeArguments.size == sourceArguments.size) {
            val arguments = typeArguments.indices.joinToString(",") {
                renderDtoType(typeArguments[it], sourceArguments[it])
            }
            "${type.canonicalText.substringBefore('<')}<$arguments>"
        } else {
            type.canonicalText
        }
        return name + if (element is KtNullableType) "?" else ""
    }

    @McpTool("explyt_trace_spring_call_chain")
    @McpDescription(
        description = "Call before changing a service method's signature or threading a new parameter through the " +
                "layers, and after explyt_find_spring_endpoint when a task needs the logic behind a route, not only " +
                "its handler. " +
                "Traces the call chain from the method at filePath:line through the Spring layers " +
                "(Controller → Service → Repository) by resolving the calls into injected beans - following an " +
                "injected interface to its implementation is where a hand-made trace usually stops. " +
                "Any line of the method identifies it - its signature, an annotation on it, or a line of its " +
                "body - so the line explyt_find_spring_endpoint reports for a handler can be passed straight in; " +
                "a line belonging to no method is refused with the nearest method declarations in that file. " +
                "Returns the chain of methods with their Spring stereotype (CONTROLLER, SERVICE, REPOSITORY, " +
                "COMPONENT, CONFIGURATION), parameters, called methods, file paths and line numbers, and with " +
                "includeTests the test files that reference the discovered methods - the tests a signature change " +
                "will break."
    )
    suspend fun traceCallChain(
        @McpDescription("Path to the source file containing the starting method (project-relative, e.g. 'src/main/kotlin/.../MyController.kt')")
        filePath: String,
        @McpDescription(
            "1-based line number of the method to start tracing from. Any line of the method works - its " +
                    "signature, an annotation on it, or a line of its body - so the line another Explyt tool " +
                    "reports for a handler can be passed through unchanged."
        )
        line: Int,
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription("How many layers deep to trace (default 3). Each layer follows method calls into injected beans.")
        depth: Int = 3,
        @McpDescription("Whether to find test files that reference the discovered methods (default true)")
        includeTests: Boolean = true,
    ): String {
        if (filePath.isBlank()) mcpFail("filePath must not be empty")
        if (line < 1) mcpFail("line must be >= 1")
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
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

                val psiMethod = methodAtLine(psiFile, document, line)
                    ?: mcpFail(noMethodAtLineMessage(psiFile, document, filePath, line))

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

    /**
     * The method declared on [line], whether that line carries its signature, its body or its annotations.
     *
     * Only the line's *first* offset used to be examined, and that offset is the indentation whitespace. Inside a
     * body that whitespace is a child of the method, so a body line resolved; on a declaration line it is a
     * sibling of the method under the class body, so the search reached the class and reported that no method
     * exists. The declaration line is the one every other tool hands out - `explyt_find_spring_endpoint` reports
     * a handler at its declaration - so the two tools disagreed about the same method.
     *
     * Scanning the line leaf by leaf rather than widening the parent search keeps a blank line between two methods
     * unresolved, which is a genuine miss and must not silently trace a neighbour.
     */
    private fun methodAtLine(psiFile: PsiFile, document: Document, line: Int): PsiMethod? {
        val lineIndex = (line - 1).coerceIn(0, document.lineCount - 1)
        val lineEnd = document.getLineEndOffset(lineIndex)
        var offset = document.getLineStartOffset(lineIndex)

        while (offset <= lineEnd) {
            ProgressManager.checkCanceled()
            val leaf = psiFile.findElementAt(offset) ?: return null
            leaf.getUastParentOfType<UMethod>()?.javaPsi?.let { return it }
            offset = maxOf(leaf.textRange.endOffset, offset + 1)
        }
        return null
    }

    /**
     * A miss names the methods around [line], because the caller cannot see which line convention was expected.
     *
     * A bare "no method found" is indistinguishable from "the file has no methods" and from "the line belongs to a
     * method this tool cannot read", and it leaves the caller re-reading the file by hand.
     */
    private fun noMethodAtLineMessage(psiFile: PsiFile, document: Document, filePath: String, line: Int): String {
        val nearest = methodDeclarationLines(psiFile, document)
            .sortedBy { abs(it.second - line) }
            .take(MAX_SUGGESTED_METHODS)
            .sortedBy { it.second }
            .joinToString { "${it.first} (line ${it.second})" }

        return if (nearest.isEmpty()) "no method found at $filePath:$line, and the file declares no methods"
        else "no method found at $filePath:$line. Nearest methods in this file: $nearest"
    }

    private fun methodDeclarationLines(psiFile: PsiFile, document: Document): List<Pair<String, Int>> {
        val declarations = mutableListOf<Pair<String, Int>>()
        psiFile.toUElement()?.accept(object : AbstractUastVisitor() {
            override fun visitMethod(node: UMethod): Boolean {
                ProgressManager.checkCanceled()
                val anchor = (node.uastAnchor?.sourcePsi ?: node.sourcePsi)?.textRange
                if (anchor != null && anchor.startOffset <= document.textLength) {
                    declarations += node.name to document.getLineNumber(anchor.startOffset) + 1
                }
                return false
            }
        })
        return declarations
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

        val position = sourcePositionOf(psiMethod, project)
        val node = CallChainNodeJson(
            layer = containingClass?.let { detectSpringLayer(it) },
            className = containingClass?.qualifiedName ?: containingClass?.name,
            methodName = psiMethod.name,
            filePath = position.filePath,
            line = position.line,
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
                val position = sourcePositionOf(refElement, project)
                val refFile = position.filePath ?: continue
                val refLine = position.line ?: continue
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
        description = "Call before writing a query, a migration, a DTO or a projection, and before adding a field to " +
                "an entity, to see the table, column and relationship names the database actually uses. " +
                "Lists the JPA entities (@Entity classes, javax.persistence and jakarta.persistence alike) of the " +
                "project, optionally under one package, with their schema metadata: class name, file path and line, " +
                "table name (from @Table or the default), fields with column names, types, primary key flag and " +
                "nullability, JPA relationships (@OneToOne, @OneToMany, @ManyToOne, @ManyToMany) with " +
                "joinColumn/mappedBy, and the indexes declared in @Table(indexes=[...]). " +
                "A column name that differs from its field name, and a relationship's owning side, are exactly what " +
                "a query written from the Java field names gets wrong."
    )
    suspend fun getSpringDataEntities(
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription("Optional fully-qualified package prefix to restrict results (e.g. 'com.example.domain'). Leave empty for all packages.")
        packageFilter: String = "",
    ): String {
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
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

        val position = sourcePositionOf(psiClass, project)
        return SpringDataEntityJson(
            name = simpleName,
            className = qualifiedName,
            filePath = position.filePath,
            line = position.line,
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

    /**
     * File path and line of a single source anchor. Both are `null` when the reported element has no
     * physical declaration to point at, so a caller never gets a path and a line taken from different files.
     */
    private data class SourcePosition(val filePath: String?, val line: Int?)

    private fun sourcePositionOf(element: PsiElement, project: Project): SourcePosition {
        val anchor = sourceAnchorOf(element) ?: return SourcePosition(null, null)
        return SourcePosition(relativePathOf(anchor, project), lineOfAnchor(anchor))
    }

    /**
     * Physical element a source position may be reported for.
     *
     * Light and synthetic members - a Kotlin `data class` `copy()`, an enum `values()`, or any light method
     * whose origin declaration is absent - have no text range, and reading a line number from them fails.
     * Such a member is reported through its declaring class, and through nothing at all when that class is
     * synthetic too.
     */
    private fun sourceAnchorOf(element: PsiElement): PsiElement? =
        element.withSourcePosition()
            ?: (element as? PsiMember)?.containingClass?.withSourcePosition()

    private fun PsiElement.withSourcePosition(): PsiElement? =
        takeIf { it.hasSourcePosition() } ?: navigationElement?.takeIf { it.hasSourcePosition() }

    private fun PsiElement.hasSourcePosition(): Boolean = textRange != null && containingFile != null

    private fun lineOfAnchor(anchor: PsiElement): Int? =
        anchor.getLineNumber(start = true).takeIf { it >= 0 }?.plus(1)

    /** 1-based line of [element], or `null` when it has no physical declaration to point at. */
    @VisibleForTesting
    internal fun lineOf(element: PsiElement): Int? = sourceAnchorOf(element)?.let(::lineOfAnchor)

    companion object {
        // Guard cap for single-target lookups (find / contract): a URL pattern is not
        // expected to resolve to many endpoints, so a small cap is enough.
        private const val MAX_ENDPOINT_RESULTS = 50

        // A miss is answered with the routes around it, and a handful of them already names the controller
        // and its conventions; more would only repeat the listing tool with a worse filter.
        private const val MAX_NEAREST_ROUTES = 20

        // Hard cap for a single page of the "list all endpoints" tool. Converting an endpoint to JSON resolves
        // PSI, so the page size bounds both the read-action duration and the response size.
        private const val MAX_ENDPOINT_LIST_RESULTS = 2000

        // Default page size: high enough to return every endpoint of a typical project in one call, low enough
        // that a broad call on a large/generated API stays cheap. Callers page via 'offset'.
        private const val DEFAULT_ENDPOINT_PAGE_SIZE = 500
        private const val MAX_ENTITY_RESULTS = 500
        private val mapper = ObjectMapper()

        // Match quality of an endpoint against the queried pattern, lowest first: the forgiving substring match
        // exists to find a route from a fragment, never to outrank the route that matches the pattern itself.
        private const val EXACT_MATCH = 0
        private const val PATTERN_MATCH = 1
        private const val SUBSTRING_MATCH = 2

        private const val COMPLETE_CONTRACT = "COMPLETE"
        private const val PARTIAL_CONTRACT = "PARTIAL"

        private const val TEMPLATE_NAME_GROUP = "name"

        // Enough to show the caller which line convention the file uses; the whole method list would bury it.
        private const val MAX_SUGGESTED_METHODS = 5

        /** Return types of a route-registration bean, whose signature describes the registration, not a request. */
        private val ROUTE_FUNCTION_TYPES = listOf(
            SpringWebClasses.ROUTE_FUNCTION,
            SpringWebClasses.SERVLET_ROUTE_FUNCTION,
        )

        private const val ATTR_NAME = "name"
        private const val ATTR_NULLABLE = "nullable"
        private const val ATTR_MAPPED_BY = "mappedBy"
        private const val ATTR_INDEXES = "indexes"
        private const val ATTR_COLUMN_LIST = "columnList"
        private const val ATTR_UNIQUE = "unique"
        private const val NOT_NULL_SIMPLE_NAME = "NotNull"

        /** Binding annotations whose parameters [extractParameters] already reports. */
        private val COLLECTED_BINDING_ANNOTATIONS = listOf(
            SpringWebClasses.PATH_VARIABLE,
            SpringWebClasses.REQUEST_PARAM,
            SpringWebClasses.REQUEST_PART,
            SpringWebClasses.REQUEST_BODY,
            SpringWebClasses.REQUEST_HEADER,
        )

        private val NAMED_BINDING_ANNOTATIONS = listOf(
            SpringWebClasses.COOKIE_VALUE,
            SpringWebClasses.MODEL_ATTRIBUTE,
            SpringWebClasses.REQUEST_PARAM,
            SpringWebClasses.REQUEST_PART,
        )

        private val MULTIPART_PART_TYPES = setOf(
            SpringWebUtil.MULTIPART_FILE,
            SpringWebClasses.JAVAX_HTTP_PART,
            SpringWebClasses.JAKARTA_HTTP_PART,
        )

        /**
         * Types Spring's own argument resolvers supply from the container rather than from a named place in the
         * request — the "Method Arguments" table of the Spring MVC reference.
         *
         * Deliberately excludes `Pageable`, `Sort` and `HttpEntity`. Those are resolved by Spring too, but they
         * *do* have a client-visible wire format (`?page=&size=&sort=`, the request body), and calling them
         * `FRAMEWORK` would tell a reader generating a client that there is nothing to send. Leaving them
         * `UNKNOWN` errs toward "look at this", which is the safe direction for this tool.
         *
         * Matched by inheritance, so `HttpServletRequest` matches `ServletRequest` and `BindingResult` matches
         * `Errors` without listing every subtype.
         */
        private val FRAMEWORK_SUPPLIED_TYPES = listOf(
            "jakarta.servlet.ServletRequest",
            "jakarta.servlet.ServletResponse",
            "jakarta.servlet.http.HttpSession",
            "jakarta.servlet.http.Part",
            "javax.servlet.ServletRequest",
            "javax.servlet.ServletResponse",
            "javax.servlet.http.HttpSession",
            "java.security.Principal",
            "java.util.Locale",
            "java.util.TimeZone",
            "java.time.ZoneId",
            "java.io.InputStream",
            "java.io.OutputStream",
            "java.io.Reader",
            "java.io.Writer",
            "org.springframework.http.HttpMethod",
            "org.springframework.ui.Model",
            "org.springframework.ui.ModelMap",
            "org.springframework.validation.Errors",
            "org.springframework.web.context.request.WebRequest",
            "org.springframework.web.util.UriComponentsBuilder",
            "org.springframework.web.servlet.mvc.support.RedirectAttributes",
            "org.springframework.web.bind.support.SessionStatus",
            "org.springframework.security.core.Authentication",
        )

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

/**
 * The project a tool call is about, or a message saying why none could be chosen.
 *
 * The rule lives in [McpProjectResolver]; here it is only mapped onto this tool family's failure channel.
 */
private fun getCurrentProject(projectPath: String?): Project? =
    (McpProjectResolver.resolve(projectPath) as? McpProjectChoice.Resolved)?.project

private fun projectProblem(projectPath: String?): String =
    when (val choice = McpProjectResolver.resolve(projectPath)) {
        is McpProjectChoice.Resolved -> "project not found"
        is McpProjectChoice.NotFound -> choice.message
        is McpProjectChoice.Ambiguous -> choice.message
    }

/**
 * The single open project declaring [applicationClassName], used only when no path was supplied.
 *
 * An explicit path always wins: searching other projects for the class name would answer about a codebase the
 * caller did not name, which is the very substitution [McpProjectResolver] exists to prevent. With the class in
 * several open projects the caller is asked for a path instead of being given the first match.
 */
private suspend fun getCurrentProjectForClass(applicationClassName: String? = null): Project? {
    applicationClassName ?: return null
    val declaring = ProjectManager.getInstance().openProjects
        .filter { !it.isDefault }
        .filter { project ->
            readAction {
                JavaPsiFacade.getInstance(project).findClass(applicationClassName, project.projectScope())
            } != null
        }
    return declaring.singleOrNull()
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
    /** `null` when the endpoint element has no physical declaration to point at. */
    val line: Int?,
    val parameters: List<EndpointParameterJson>,
    val returnType: String?,
    val endpointType: String,
)

/**
 * One endpoint without its per-method signature, for the `compact` listing.
 *
 * Not [EndpointJson] with nulled-out fields: the two arrays are *absent* rather than empty, so a caller cannot
 * mistake a projection for an endpoint that genuinely takes no parameters — the same absent-versus-empty
 * confusion that made a dropped parameter unreadable before.
 */
data class CompactEndpointJson(
    val httpMethods: List<String>,
    val fullPath: String,
    val controllerClass: String?,
    val methodName: String?,
    val filePath: String?,
    /** `null` when the endpoint element has no physical declaration to point at. */
    val line: Int?,
    val endpointType: String,
)

data class EndpointListJson<T>(
    val totalCount: Int,
    val offset: Int,
    val truncated: Boolean,
    val endpoints: List<T>,
)

/**
 * The result of resolving one URL pattern, for the find and contract tools.
 *
 * [endpoints] is ordered by [EndpointPathPatterns.SPECIFICITY], the order Spring picks a handler in, so its
 * first element is the one that dispatches when several routes match. [nearestByPrefix] is filled only when
 * [endpoints] is empty: the routes sharing the longest leading path ([sharedPrefix]) with the pattern, which is
 * where a route that does not exist yet would be added. The two lists are empty rather than null so a client
 * can always iterate them; [sharedPrefix] is null exactly when [nearestByPrefix] is empty.
 */
data class EndpointLookupJson<T>(
    val totalCount: Int,
    val truncated: Boolean,
    val endpoints: List<T>,
    val sharedPrefix: String?,
    val nearestByPrefix: List<CompactEndpointJson>,
)

data class EndpointParameterJson(
    val name: String,
    /**
     * Where the value comes from: `PATH`, `QUERY`, `BODY`, `HEADER`, `COOKIE` or `MODEL` for an annotation-bound
     * parameter, `FRAMEWORK` for one the container supplies, and `UNKNOWN` for one this tool cannot classify —
     * typically bound by a project-local `HandlerMethodArgumentResolver`.
     */
    val source: String,
    val type: String,
    /** `null` when nothing declares it, which is every source the tool cannot read the contract of. */
    val required: Boolean?,
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
    /** `null` for a light or synthetic method with no physical declaration, e.g. a generated `copy()`. */
    val line: Int?,
    val parameters: List<String>,
    val callsInto: List<CallTargetJson>,
)

data class CallTargetJson(
    val target: String,
    /** `null` for a light or synthetic call target with no physical declaration. */
    val line: Int?,
)

data class ServiceCallJson(
    val target: String,
    val filePath: String?,
    val line: Int?,
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
    /** `null` when the endpoint method has no physical declaration to point at. */
    val line: Int?,
    val parameters: List<EndpointParameterJson>,
    val returnType: String?,
    val responseSchema: DtoSchemaJson?,
    val produces: List<String>,
    val consumes: List<String>,
    val serviceCall: ServiceCallJson?,
    val endpointType: String,
    /**
     * `COMPLETE` when a request-handling method declares the endpoint, `PARTIAL` when the endpoint exists but has
     * no such signature to read - a functional route or an OpenAPI declaration.
     *
     * A `PARTIAL` contract reports only what the declaration states: the path, the verb, the declaring element and
     * the parameters the URL template names. Its empty `parameters` or null `returnType` mean "not declared here",
     * never "the endpoint takes nothing".
     */
    val contractStatus: String,
    /** What has to be read elsewhere, and where. `null` for a `COMPLETE` contract. */
    val contractUnavailableReason: String?,
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
    /** `null` when the entity class has no physical declaration to point at. */
    val line: Int?,
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

