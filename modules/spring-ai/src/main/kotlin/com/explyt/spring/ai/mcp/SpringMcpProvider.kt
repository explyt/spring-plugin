/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.jpa.JpaClasses
import com.explyt.spring.core.SpringCoreClasses

import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.ai.mcp.entities.EntityInventory
import com.explyt.spring.ai.mcp.entities.EntityRecord
import com.explyt.spring.ai.mcp.entities.EntitySchema
import com.explyt.spring.core.service.beans.BeanQueryException
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.util.ApplicationBasePath
import com.explyt.spring.web.util.EndpointPathPatterns
import com.explyt.spring.web.util.HandlerSignature
import com.explyt.spring.web.util.EndpointPathPatterns.PathReading
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.WebApplicationStack
import com.explyt.util.ExplytAnnotationUtil.findFirstAnnotation
import com.explyt.util.ExplytAnnotationUtil.getBooleanAttribute
import com.explyt.util.ExplytAnnotationUtil.getMemberValues
import com.explyt.util.ExplytAnnotationUtil.getStringAttribute
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.fasterxml.jackson.annotation.JsonInclude
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.codeInspection.isInheritorOf
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue.TRUE
import com.intellij.mcpserver.annotations.McpToolHints
import com.intellij.mcpserver.mcpFail

import com.intellij.openapi.application.smartReadAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.module.Module
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
import java.util.IdentityHashMap
import kotlin.math.abs
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.annotations.VisibleForTesting

import org.jetbrains.kotlin.idea.base.util.projectScope
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UResolvable
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.evaluateString
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.getUastParentOfType
import org.jetbrains.uast.toUElement

/**
 * What every tool of this family says about its project argument.
 *
 * One text for all of them: a caller who learns the rule from one tool can rely on it for the rest.
 */
private const val PROJECT_PATH_DESCRIPTION =
    "Path to the project root. Omit it when a single project is open; when several are, it is required, " +
            "and a path naming none of them is refused rather than answered from another one."

private val HTTP_METHODS = listOf("GET", "POST", "PUT", "DELETE", "PATCH", "HEAD", "OPTIONS")

private const val HTTP_METHOD_DESCRIPTION =
    "Optional HTTP method filter, case-insensitive: GET, POST, PUT, DELETE, PATCH, HEAD, OPTIONS. Leave empty to " +
            "match all methods. Any other value is rejected with the list of valid values."

/** The values `explyt_get_spring_http_endpoints` can filter by: the endpoint types it lists at all. */
private val WEB_ENDPOINT_TYPES = EndpointType.entries.filter { it.isWeb }.map { it.name }

class SpringBootApplicationMcpToolset : McpToolset {

    @McpTool("explyt_get_spring_boot_applications", title = "Spring Boot applications in the project")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
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

    @McpTool("explyt_get_project_beans_by_spring_boot_application", title = "Beans of a Spring Boot application by stereotype")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Call before adding a component, to see which beans of that stereotype already exist and " +
                "what they are named; before injecting by type, to check that exactly one candidate exists; and " +
                "when asked what a module contributes to the context. " +
                "Lists the beans of one Spring Boot application, filtered by stereotype, from the IDE's bean " +
                "model: it includes @Bean factory methods, meta-annotated stereotypes and @Import-ed configurations, " +
                "which a text search for '@Service' or '@Component' never finds. " +
                "Returns each bean's name, fully-qualified class and module, one row per name a bean answers to. " +
                "Every row also names the model that answered in 'source': STATIC is an estimate of the module, " +
                "NATIVE_SNAPSHOT a context recorded at 'snapshotImportedAt' (ISO-8601 UTC, absent when unknown) " +
                "and never live, with 'contextId' naming it. 'limitations' appears only on a row whose own bean " +
                "has one, such as ALIASES_NOT_EXPORTED (the bean may answer to names the context did not export). " +
                "When sources changed after 'snapshotImportedAt', pass source=STATIC for an answer read from them. " +
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
                    "COMPONENT - for Spring Components/Service and other beans. \n" +
                    "Case-insensitive; any other value is rejected with the list of valid values."
        )
        beanType: String,
        @McpDescription(
            "Which bean model answers, case-insensitive: AUTO (default) prefers a loaded application context and " +
                    "falls back to the static model, STATIC always uses the static model, NATIVE requires a loaded " +
                    "context. Any other value is rejected with the list of valid values."
        )
        source: String = "AUTO",
        @McpDescription("Id of the loaded context to answer from, when several are loaded for this application.")
        contextId: String? = null,
    ): String {
        val project = getCurrentProject(projectPath)
            ?: projectPath?.takeIf { it.isNotBlank() }?.let { mcpFail(projectProblem(it)) }
            ?: getCurrentProjectForClass(applicationClassName)
            ?: mcpFail(projectProblem(projectPath))
        val mcpBeanType = McpBeanTypes.valueOf(
            McpChoiceArguments.required(beanType, "beanType", McpBeanTypes.entries.map { it.name })
        )
        val preference = BeanSourcePreference.valueOf(
            McpChoiceArguments.required(source, "source", BeanSourcePreference.entries.map { it.name })
        )
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
            .map {
                McpSpringBean(
                    it.beanName, it.className, it.moduleName, it.source, it.contextId, it.snapshotImportedAt,
                    it.limitations
                )
            }
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


    @McpTool("explyt_find_spring_endpoint", title = "Resolve a URL to its Spring handler")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Call when a task starts from a URL - a bug report, a frontend call, a log line, a curl - " +
                "and right after adding or changing a mapping, to confirm the composed path and the bound parameters " +
                "registered as intended. " +
                "Resolves the URL against the IDE's endpoint model, not the source text: the path is composed from " +
                "the class-level @RequestMapping and the method-level @GetMapping/@PostMapping, so it never appears " +
                "as one string and a text search for it finds nothing, and a class-level prefix can silently give a " +
                "new method a different URL than the one written on it. " +
                "Covers Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign, OpenAPI, message brokers " +
                "(Kafka/RabbitMQ listeners), event listeners and Actuator - the project's own @Endpoint classes " +
                "and, when Spring Boot's endpoint auto-configuration is on the classpath, the built-in ones such as " +
                "'/actuator/health'. An Actuator endpoint carries 'exposed': EXPOSED or NOT_EXPOSED as " +
                "management.endpoints.web.exposure.include/exclude decide it (by default only health), UNKNOWN " +
                "when a value cannot be read from the configuration files; it is found whatever 'exposed' says, " +
                "since a profile or an environment variable can change exposure at run time. " +
                "Returns an object with 'totalCount' (how many endpoints matched), 'truncated' (true when more " +
                "matched than were returned), 'endpoints' and 'nearestByPrefix'. Each endpoint carries full path, " +
                "HTTP methods, controller class, method name, parameters with their binding source, return type, " +
                "file path, line and endpoint type, and 'consumes'/'produces' when the mapping declares media " +
                "types. 'endpoints' lists the closest match to the pattern first: an exact path, then one matching " +
                "it as a pattern, then one merely containing it, and within each group by path specificity, the " +
                "way Spring ranks path patterns - literal before '{template}', fewer wildcards first - so of routes " +
                "with different paths matching one URL the first is the one that dispatches. Handlers sharing one " +
                "path and verb are not ordered by dispatch: the request's Content-Type and Accept choose among " +
                "them, by the 'consumes' and 'produces' each record carries. " +
                "Covers annotation-declared handlers and functional routes alike; for a functional route the " +
                "controller class and method name are the bean factory that registers it, and 'parameters' holds " +
                "the path variables its URL template declares. " +
                "Matching is forgiving: 'requests' matches '/api/.../requests', and '{id}' matches any path variable. " +
                "A deployed URL can be passed as it is: the scheme, host, query and fragment are ignored. When no " +
                "route answers the path as written, a base path the module's configuration declares - " +
                "server.servlet.context-path followed by spring.mvc.servlet.path, or spring.webflux.base-path - is " +
                "stripped and reported as 'basePath', a declared fact; failing that, leading segments are dropped " +
                "until a route answers - a context path or a gateway prefix living only in deployment " +
                "configuration - and the dropped part is reported as 'assumedPrefix', a guess. Both are null when " +
                "the path matched as written. " +
                "'fullPath' is the path the application serves, with configuration placeholders such as " +
                "'\${app.path:/l}' resolved; 'pathTemplate' holds the path as declared and is present only when it " +
                "differs from 'fullPath'. " +
                "When 'endpoints' is empty, no route answers the URL even without a leading prefix, and " +
                "'nearestByPrefix' lists the existing routes that share the longest leading path with it - the " +
                "controller and the conventions a new route has to fit; 'sharedPrefix' names that common path as " +
                "the routes declare it, with their '{templates}'."
    )
    suspend fun findEndpoint(
        @McpDescription(
            "URL or URL pattern to search for. Can be: " +
                    "a URL copied from a browser, a log line or a curl, like " +
                    "'https://example.com/ctx/api/orgs/42/drilldown/7/requests?from=1d', " +
                    "a full path like '/api/orgs/{orgId}/drilldown/{metricId}/requests', " +
                    "a partial path like '/drilldown/requests' or just 'requests', " +
                    "or a path with wildcards like '/{id}/requests'. " +
                    "Path variables like {orgId} are treated as wildcards matching any segment."
        )
        urlPattern: String,
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription(HTTP_METHOD_DESCRIPTION)
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
     * The URL is reduced to its request path and read as written first, then under a base path a module's
     * configuration declares ([DeclaredBasePaths]), then under ever longer leading prefixes no route declares
     * ([EndpointPathPatterns.readingsOf]); the first reading any route answers is the answer, and the prefix it
     * dropped is reported rather than hidden - as [EndpointLookupJson.basePath] when the configuration declares it,
     * as [EndpointLookupJson.assumedPrefix] when it is a guess. A deployed URL carries its context path, and without
     * this a real, working URL answered "no such route" - which invites the caller to write a duplicate.
     *
     * A URL that matches nothing is not a dead end: [EndpointLookupJson.nearestByPrefix] carries the routes sharing
     * the longest leading path with it, which is where a route that does not exist yet would be added.
     */
    private suspend fun <T> lookupEndpoints(
        urlPattern: String,
        projectPath: String?,
        httpMethod: String,
        toJson: (EndpointElement, Project) -> T,
    ): EndpointLookupJson<T> {
        if (urlPattern.isBlank()) mcpFail("urlPattern must not be empty")
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
        val requestPath = EndpointPathPatterns.requestPathOf(urlPattern)
        val pathReadings = EndpointPathPatterns.readingsOf(requestPath).toList()
        val methodFilter = McpChoiceArguments.optional(httpMethod, "httpMethod", HTTP_METHODS)

        return withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val allEndpoints = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
                val basePaths = DeclaredBasePaths(allEndpoints)
                // Lazy: a URL matching as written never pays for reading the modules' configuration.
                val readings = sequence {
                    yield(LookupReading(pathReadings.first()))
                    yieldAll(basePaths.readingsOf(requestPath))
                    yieldAll(pathReadings.drop(1).map(::LookupReading))
                }

                // The reading is settled by the path alone: a URL that resolves only for another verb has been found,
                // and dropping one more segment of it to satisfy the method filter would answer a different URL.
                val answered = readings.firstNotNullOfOrNull { reading ->
                    matchesOf(allEndpoints, reading, basePaths).takeIf { it.isNotEmpty() }?.let { reading to it }
                }
                val matching = answered?.second.orEmpty().filter { endpoint ->
                    methodFilter == null || endpoint.requestMethods.isEmpty()
                            || endpoint.requestMethods.any { it.equals(methodFilter, ignoreCase = true) }
                }
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
                val nearest = if (matching.isEmpty()) {
                    nearestByPrefix(allEndpoints, answered?.first?.let(::listOf) ?: readings.toList(), basePaths, project)
                } else {
                    null
                }
                val settled = answered?.first ?: nearest?.reading

                EndpointLookupJson(
                    totalCount = matching.size,
                    truncated = matching.size > MAX_ENDPOINT_RESULTS,
                    endpoints = page,
                    basePath = settled?.basePath,
                    assumedPrefix = settled?.pathReading?.assumedPrefix,
                    sharedPrefix = nearest?.sharedPrefix,
                    nearestByPrefix = nearest?.routes.orEmpty(),
                )
            }
        }
    }

    /**
     * The endpoints answering [reading], closest match first.
     *
     * Matches are ordered by how closely they answer the path ([matchRank]) and then by
     * [EndpointPathPatterns.SPECIFICITY], so when a literal route and a `{template}` route both match, the first
     * element is the one Spring dispatches to. Handlers sharing one path keep no dispatch order: Spring chooses among
     * them by the request's media types, which [CompactEndpointJson.consumes] and [CompactEndpointJson.produces] show.
     */
    private fun matchesOf(
        endpoints: List<EndpointElement>,
        reading: LookupReading,
        basePaths: DeclaredBasePaths,
    ): List<EndpointElement> =
        endpoints.asSequence()
            .onEach { ProgressManager.checkCanceled() }
            .map { it to SpringWebUtil.simplifyUrl(it.path) }
            .filter { (endpoint, path) -> admits(reading, endpoint, path, basePaths) }
            .mapNotNull { (endpoint, path) -> matchRank(path, reading)?.let { RankedEndpoint(endpoint, path, it) } }
            .sortedWith(compareBy<RankedEndpoint> { it.rank }.thenBy(EndpointPathPatterns.SPECIFICITY) { it.path })
            .map { it.endpoint }
            .toList()

    private data class RankedEndpoint(val endpoint: EndpointElement, val path: String, val rank: Int)

    /**
     * One way a request path meets the routes: as written, under a [basePath] a module's configuration declares, or
     * under a guessed prefix no route declares ([PathReading.assumedPrefix]).
     */
    private data class LookupReading(val pathReading: PathReading, val basePath: String? = null) {
        val path: String get() = pathReading.path
        val isAsWritten: Boolean get() = basePath == null && pathReading.assumedPrefix == null
    }

    /**
     * Whether the route at [route] may answer [reading]: under a declared base path only a route of a module that
     * declares that very base path does - another application does not serve under it, and matching its routes
     * would report a fact about the wrong module. A guessed prefix keeps the rule of [PathReading.admits].
     */
    private fun admits(
        reading: LookupReading,
        endpoint: EndpointElement,
        route: String,
        basePaths: DeclaredBasePaths,
    ): Boolean =
        if (reading.basePath != null) basePaths.of(endpoint) == reading.basePath
        else reading.pathReading.admits(route)

    /**
     * The base path each module's configuration declares ([ApplicationBasePath]), read at most once per module and
     * per lookup: reading it scans the module's configuration files.
     */
    private class DeclaredBasePaths(private val endpoints: List<EndpointElement>) {

        private val byModule = HashMap<Module, String>()
        private val byEndpoint: Map<EndpointElement, String> by lazy {
            endpoints.mapNotNull { endpoint -> declaredFor(endpoint)?.let { endpoint to it } }
                .toMap(IdentityHashMap())
        }

        fun of(endpoint: EndpointElement): String? = byEndpoint[endpoint]

        /**
         * The readings of [requestPath] under each declared base path it opens with at a segment boundary, the
         * longest base path first: `/t/api/items` under `/t/api` before `/t`.
         */
        fun readingsOf(requestPath: String): List<LookupReading> =
            byEndpoint.values.distinct()
                .filter { requestPath == it || requestPath.startsWith("$it/") }
                .sortedByDescending { it.length }
                .map { basePath ->
                    val rest = requestPath.removePrefix(basePath).ifEmpty { "/" }
                    LookupReading(PathReading(assumedPrefix = null, path = rest), basePath)
                }

        private fun declaredFor(endpoint: EndpointElement): String? {
            val module = moduleOf(endpoint) ?: return null
            return byModule.getOrPut(module) { ApplicationBasePath.of(module).orEmpty() }.ifEmpty { null }
        }

        /**
         * The module an endpoint is served by. A built-in Actuator endpoint is declared in a jar, where
         * `findModuleForPsiElement` answers `null` for anything but a file, so its file is asked instead. That names one
         * of the modules the library is attached to - the first in dependency order - and the endpoint is read under
         * that module's base path; in a project whose applications share the jar but declare different base paths, the
         * others are not tried.
         */
        private fun moduleOf(endpoint: EndpointElement): Module? =
            ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement)
                ?: endpoint.psiElement.containingFile?.originalFile?.let { ModuleUtilCore.findModuleForPsiElement(it) }
    }

    /**
     * The routes around a URL no route answers, read the way that shares the longest leading path with them.
     *
     * Each reading is scored by its longest shared path, and the reading dropping the fewest segments wins a tie:
     * a context path the caller included would otherwise leave the neighbourhood empty, because no route opens
     * with it. Nothing is returned when no reading shares a segment: every route in the project "shares" the root,
     * and listing all of them would say nothing about where the URL belongs.
     *
     * The shared path is spelled the way the most specific neighbour declares it: the URL of a request carries values
     * where a route has `{templates}`, and echoing `/api/short-links/15a4a137` would read as if a route with that
     * literal id existed.
     */
    private fun nearestByPrefix(
        endpoints: List<EndpointElement>,
        readings: List<LookupReading>,
        basePaths: DeclaredBasePaths,
        project: Project,
    ): Neighbourhood? {
        val (reading, sharedSegments, nearest) = readings.asSequence()
            .mapNotNull { reading ->
                endpoints
                    .filter { admits(reading, it, SpringWebUtil.simplifyUrl(it.path), basePaths) }
                    .groupBy { EndpointPathPatterns.sharedLeadingSegments(SpringWebUtil.simplifyUrl(it.path), reading.path) }
                    .maxByOrNull { it.key }
                    ?.takeIf { it.key > 0 }
                    ?.let { Triple(reading, it.key, it.value) }
            }
            .maxByOrNull { it.second }
            ?: return null

        val ordered = nearest.sortedWith(compareBy(EndpointPathPatterns.SPECIFICITY) { SpringWebUtil.simplifyUrl(it.path) })
        val compact = ordered
            .take(MAX_NEAREST_ROUTES)
            .onEach { ProgressManager.checkCanceled() }
            .map { toCompactEndpointJson(it, project) }
        val sharedPrefix = EndpointPathPatterns.prefixOf(SpringWebUtil.simplifyUrl(ordered.first().path), sharedSegments)
        return Neighbourhood(reading, sharedPrefix, compact)
    }

    private data class Neighbourhood(
        val reading: LookupReading,
        val sharedPrefix: String,
        val routes: List<CompactEndpointJson>,
    )

    /**
     * How closely a route at [endpointPath] answers [reading], lowest first, or `null` when it does not answer it.
     *
     * Matching is deliberately forgiving - a bare `items` finds `/api/demo/items` - so a long unrelated path that
     * merely contains the pattern matches too. Ordering those by path specificity alone puts such a path *above*
     * the route matching the pattern exactly, because specificity breaks ties by descending length. The caller
     * reads its answer off the first element, so how well an endpoint fits the query has to outrank how Spring
     * would dispatch between the endpoints that fit it equally well.
     *
     * The substring match finds a route from a fragment the caller wrote. Under a base path or an assumed prefix the
     * rest of the URL is what the application routes on, not a fragment, so only a route matching it whole counts.
     */
    private fun matchRank(endpointPath: String, reading: LookupReading): Int? = when {
        endpointPath == reading.path -> EXACT_MATCH
        SpringWebUtil.isEndpointMatches(endpointPath, reading.path) -> PATTERN_MATCH
        reading.isAsWritten && endpointPath.contains(reading.path) -> SUBSTRING_MATCH
        else -> null
    }

    private fun toCompactEndpointJson(endpoint: EndpointElement, project: Project): CompactEndpointJson {
        val declaringMethod = declaringMethodOf(endpoint)
        val controllerClass = endpoint.containingClass ?: declaringMethod?.containingClass
        val position = sourcePositionOf(endpoint.psiElement, project)
        // A loader may know the declaring file of an endpoint whose element has no source position of its own.
        val filePath = position.filePath
            ?: endpoint.containingFile?.let { relativePathOf(it, project) }
        val mediaTypes = mediaTypesOf(
            requestHandlerOf(endpoint)?.toUElement() as? UMethod,
            ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement),
            project,
        )

        return CompactEndpointJson(
            httpMethods = endpoint.requestMethods.ifEmpty { listOf("ALL") },
            fullPath = endpoint.path,
            pathTemplate = endpoint.pathTemplate.takeIf { it != endpoint.path },
            controllerClass = controllerClass?.qualifiedName,
            methodName = declaringMethod?.name,
            filePath = filePath,
            line = position.line,
            endpointType = endpoint.type.readable,
            consumes = mediaTypes.consumes,
            produces = mediaTypes.produces,
            exposed = endpoint.exposure?.name,
        )
    }

    private fun toEndpointJson(endpoint: EndpointElement, project: Project): EndpointJson {
        val handler = requestHandlerOf(endpoint)
        val core = toCompactEndpointJson(endpoint, project)

        return EndpointJson(
            httpMethods = core.httpMethods,
            fullPath = core.fullPath,
            pathTemplate = core.pathTemplate,
            controllerClass = core.controllerClass,
            methodName = core.methodName,
            filePath = core.filePath,
            line = core.line,
            parameters = handler?.let { extractParameters(it) } ?: pathTemplateParameters(endpoint.path),
            returnType = handler?.let(HandlerSignature::declaredReturnType)?.canonicalText,
            endpointType = core.endpointType,
            consumes = core.consumes,
            produces = core.produces,
            exposed = core.exposed,
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
        val multipartRequestNames = HandlerSignature.requestParameters(psiMethod).asSequence()
            .filter { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_PARAM) && isMultipartPart(it.type) }
            .map(::wireNameOf)
            .toSet()
        val servletApplication = isServletApplication(psiMethod)
        for (info in SpringWebUtil.collectRequestParameters(psiMethod)) {
            val source = if (servletApplication && info.name in multipartRequestNames) "PART" else "QUERY"
            result += EndpointParameterJson(info.name, source, info.typeFqn, info.isRequired, info.defaultValue)
        }
        for (param in HandlerSignature.requestParameters(psiMethod)) {
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

    /**
     * Whether the handler's application runs on servlet MVC, where `@RequestParam` also binds a multipart part.
     * A project carrying WebFlux next to it - usually only for `WebClient` - is still a servlet application.
     */
    private fun isServletApplication(psiMethod: PsiMethod): Boolean {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return false
        return WebApplicationStack.of(module) == WebApplicationStack.SERVLET
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
        HandlerSignature.requestParameters(psiMethod)
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

    @McpTool("explyt_get_spring_http_endpoints", title = "HTTP endpoints of one controller or the whole project")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Lists the HTTP endpoints of one controller (controllerFilter) or of the whole project. " +
                "Call before extending an existing controller: the listing shows its class-level prefix, its sibling " +
                "routes and the parameter conventions a new route must match, and a literal route that would " +
                "compete with a '{template}' sibling. " +
                "Call with compact=true for a first inventory of an API surface you do not know yet. " +
                "Covers Spring MVC, WebFlux, JAX-RS, HttpExchange, OpenFeign and Actuator: the project's own " +
                "@Endpoint classes and, when Spring Boot's endpoint auto-configuration is on the classpath, the " +
                "built-in ones (health, info, metrics...). An Actuator endpoint carries 'exposed': EXPOSED or " +
                "NOT_EXPOSED as management.endpoints.web.exposure.include/exclude decide it (by default only " +
                "health), UNKNOWN when a value cannot be read from the configuration files. Nothing is hidden: a " +
                "profile or an environment variable can change exposure at run time. Other endpoints have no " +
                "'exposed' key. " +
                "Returns an object with 'totalCount' (how many endpoints matched the filters), 'offset' (the index " +
                "the returned page starts at), 'truncated' (true when more matches remain after this page), and " +
                "'endpoints'. One endpoint looks exactly like this - note 'httpMethods' is an array, and the keys " +
                "are 'fullPath' and 'line', not 'path' or 'lineNumber': " +
                "{\"httpMethods\":[\"GET\"],\"fullPath\":\"/api/demo/items/{id}\"," +
                "\"controllerClass\":\"com.example.app.web.DemoController\",\"methodName\":\"getItem\"," +
                "\"filePath\":\"src/main/java/com/example/app/web/DemoController.java\",\"line\":40," +
                "\"parameters\":[{\"name\":\"id\",\"source\":\"PATH\",\"type\":\"java.lang.Long\"," +
                "\"required\":true,\"defaultValue\":null}],\"returnType\":\"com.example.app.dto.DemoDto\"," +
                "\"endpointType\":\"Spring MVC\"}. " +
                "'consumes' and 'produces' are present only when the mapping declares media types: two handlers " +
                "sharing a path and a verb are told apart by them, not by their order. " +
                "'fullPath' has configuration placeholders resolved; an endpoint declared with one, such as " +
                "'\${app.path:/l}/{code}', also carries 'pathTemplate' with the declaration as written - the key is " +
                "absent otherwise. " +
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
            "Optional endpoint type filter, case-insensitive. Possible values: SPRING_MVC, SPRING_WEBFLUX, " +
                    "SPRING_JAX_RS, SPRING_HTTP_EXCHANGE, SPRING_OPEN_FEIGN, ACTUATOR. Leave empty for all. " +
                    "Any other value is rejected with the list of valid values."
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
        val typeFilter = McpChoiceArguments.optional(endpointType, "endpointType", WEB_ENDPOINT_TYPES)
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

    @McpTool("explyt_get_spring_endpoint_contract", title = "Full request/response contract of one endpoint")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Call before writing a client, a test, a frontend call or an OpenAPI description against one " +
                "endpoint, and before changing its request or response shape, to see what callers currently depend " +
                "on. " +
                "Returns the full API contract of the endpoint: HTTP method, full path (configuration placeholders " +
                "resolved, with 'pathTemplate' holding the declared path only when it differs), every declared handler " +
                "parameter with its type, return type, response DTO field schema as Jackson writes it (recursively " +
                "expanded up to 3 levels: a @JsonProperty name in 'name' with the declared one in 'declaredName', " +
                "no transient or @JsonIgnore members, an enum as its wire values in 'enumValues' or, with " +
                "@JsonValue, as the 'jsonValue' member, its 'valueType' and the constant names in 'enumConstants', " +
                "which are not the wire values), produces/consumes media types, and 'serviceCalls': every " +
                "call the handler makes on an injected project bean - including a bean method passed as a callable " +
                "reference, such as 'validator::validate' - in source order, each with its 'target', the " +
                "'filePath' and 'line' of the target's declaration and the 'callLine' in the handler (empty when " +
                "there is none). 'serviceCall' is the first of them, kept for compatibility - often a guard or a " +
                "resolver called before the service that handles the request, not that service; to follow the " +
                "request through the layers, call explyt_trace_spring_call_chain on the handler. Calls into the " +
                "JDK, Kotlin and Spring are never listed. Reading the handler signature by hand misses what Spring " +
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
                "the handler function as the one entry of 'serviceCalls' and as 'serviceCall', and the path " +
                "variables the URL template declares, and " +
                "'contractUnavailableReason' says what has to be read at the source - an empty 'parameters' there " +
                "means 'not declared here', never 'the endpoint takes nothing'. " +
                "Returns the same object shape as explyt_find_spring_endpoint - 'totalCount', 'truncated', " +
                "'endpoints' with the closest match to the pattern first, 'basePath' when a base path the " +
                "configuration declares was stripped from the URL, 'assumedPrefix' when an undeclared leading path " +
                "had to be guessed and dropped, and 'nearestByPrefix' with 'sharedPrefix' when nothing " +
                "matched - with a contract in place of each endpoint; every counted endpoint is returned, so " +
                "endpoints.size equals totalCount unless truncated. " +
                "Take the urlPattern from explyt_find_spring_endpoint or explyt_get_spring_http_endpoints."
    )
    suspend fun getEndpointContract(
        @McpDescription(
            "URL pattern of the endpoint to inspect (e.g. '/api/orgs/{orgId}/project-success/v1/coverage/users'). " +
                    "Should match a single endpoint. If several match, all are returned, the most specific path " +
                    "first; handlers sharing a path and a verb differ by 'consumes'/'produces', and the request's " +
                    "Content-Type and Accept choose among them."
        )
        urlPattern: String,
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription(HTTP_METHOD_DESCRIPTION)
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
        val core = toCompactEndpointJson(endpoint, project)

        val serviceCalls = when {
            handler != null && uHandler != null && module != null ->
                injectedBeanCallsOf(handler, uHandler, module, project)
            else -> listOfNotNull(routeHandlerCall(endpoint, project))
        }

        return EndpointContractJson(
            httpMethods = core.httpMethods,
            fullPath = core.fullPath,
            pathTemplate = core.pathTemplate,
            controllerClass = core.controllerClass,
            methodName = core.methodName,
            filePath = core.filePath,
            line = core.line,
            parameters = handler?.let { extractParameters(it) } ?: pathTemplateParameters(endpoint.path),
            returnType = handler?.let(HandlerSignature::declaredReturnType)?.canonicalText,
            responseSchema = handler?.let(HandlerSignature::declaredReturnType)
                ?.let { ResponseSchemaReader.schemaOf(it, depth = 3) },
            produces = core.produces,
            consumes = core.consumes,
            serviceCall = serviceCalls.firstOrNull(),
            serviceCalls = serviceCalls,
            endpointType = endpoint.type.readable,
            exposed = core.exposed,
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

    /**
     * Every call the handler makes on an injected project bean, in source order, one entry per callee and line.
     *
     * All of them, because the first is often not the one that handles the request: a handler commonly resolves a
     * tenant, checks access or normalises an argument through another bean before it calls the service. A call on a
     * local alias of the bean - `val stats = statsService ?: throw ...` - is a call on the bean, and so is a bean
     * method passed as a callable reference, `input.use(validator::validate)`, which the receiving function invokes.
     * Source order is the order of the UAST visit, so an outer call precedes the calls and references in its
     * arguments.
     */
    private fun injectedBeanCallsOf(
        psiMethod: PsiMethod,
        uMethod: UMethod,
        module: com.intellij.openapi.module.Module,
        project: Project,
    ): List<ServiceCallJson> {
        val controllerClass = psiMethod.containingClass ?: return emptyList()
        val beanClasses = SpringSearchService.getInstance(project).getProjectBeans(module).map { it.psiClass }
        val beanFields = controllerClass.allFields.filter { field ->
            if (!InjectedDependencies.isInjected(field)) return@filter false
            val fieldClass = (field.type as? PsiClassType)?.resolve() ?: return@filter false
            beanClasses.any { InheritanceUtil.isInheritorOrSelf(it, fieldClass, true) }
        }.toSet()
        if (beanFields.isEmpty()) return emptyList()

        val calls = mutableListOf<ServiceCallJson>()
        fun collect(site: MethodCallSite?) {
            if (site != null) serviceCallOf(site, controllerClass, beanFields, project)?.let { calls += it }
        }
        uMethod.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                collect(MethodCallSite.of(node))
                return false
            }

            override fun visitCallableReferenceExpression(node: UCallableReferenceExpression): Boolean {
                collect(MethodCallSite.of(node))
                return false
            }
        })
        return calls.distinctBy { it.target to it.callLine }
    }

    /**
     * A member the JDK, the Kotlin standard library or Spring declares in a library: `toString`, or a method a Spring
     * Data repository inherits. A class declared in the project is never one, whatever its package.
     */
    private fun isPlatformOrFrameworkLibraryMember(declaringClass: PsiClass): Boolean {
        if (ProjectSources.declares(declaringClass)) return false
        val fqn = declaringClass.qualifiedName ?: return true
        return FRAMEWORK_PACKAGES.any(fqn::startsWith)
    }

    /** The service call [site] makes, when it invokes a project method of one of the controller's injected beans. */
    private fun serviceCallOf(
        site: MethodCallSite,
        controllerClass: PsiClass,
        beanFields: Set<PsiField>,
        project: Project,
    ): ServiceCallJson? {
        val field = InjectedDependencies.fieldOf(site.receiver, controllerClass)?.takeIf { it in beanFields } ?: return null
        val callee = site.callee
        val receiverClass = (field.type as? PsiClassType)?.resolve() ?: return null
        val calleeClass = callee.containingClass ?: return null
        if (callee.hasModifierProperty(PsiModifier.STATIC)
            || isPlatformOrFrameworkLibraryMember(calleeClass)
            || !InheritanceUtil.isInheritorOrSelf(receiverClass, calleeClass, true)
        ) return null
        val position = sourcePositionOf(callee, project)
        return ServiceCallJson(
            target = "${callee.containingClass?.qualifiedName}.${callee.name}",
            filePath = position.filePath,
            line = position.line,
            callLine = site.line,
        )
    }


    @McpTool("explyt_trace_spring_call_chain", title = "Controller → Service → Repository call chain of a method")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Call before changing a service method's signature or threading a new parameter through the " +
                "layers, and after explyt_find_spring_endpoint when a task needs the logic behind a route, not only " +
                "its handler. " +
                "Traces the call chain from the method at filePath:line through the Spring layers " +
                "(Controller → Service → Repository), following the calls into project code only - a call " +
                "written against an interface is followed to each of its implementations in the project, which " +
                "is where a hand-made trace usually stops. Calls into the JDK, the Kotlin standard library and " +
                "framework jars are left out, except where the request leaves the application: a library method " +
                "called on an injected dependency, such as JdbcTemplate.query, and a method the framework " +
                "implements, such as a Spring Data repository method, are listed as EXTERNAL and not followed. " +
                "Any line of the method identifies it - its signature, an annotation on it, or a line of its " +
                "body - so the line explyt_find_spring_endpoint reports for a handler can be passed straight in; " +
                "a line belonging to no method is refused with the nearest method declarations in that file. " +
                "Returns {status, chainLimitReached, revision, totalCount, offset, truncated, nextOffset, chain}. " +
                "'chain' holds the traced methods, the starting method first, each with an 'id', the Spring " +
                "stereotype of its class in 'layer' (CONTROLLER, SERVICE, REPOSITORY, COMPONENT, CONFIGURATION; " +
                "null for a class that is not a bean, such as a Kotlin object), 'reachedBy' (INTERNAL for a helper " +
                "of its caller's class, PROJECT for a call from another class, null for the starting method), " +
                "parameters as declared, file path and declaration line, 'aop' - the @Transactional, @Async and " +
                "cache annotations declared on the method or its class, with 'declaredOn' METHOD or CLASS; Spring " +
                "applies them through a proxy, so they do not take effect for a call reached INTERNAL, which is " +
                "a self-invocation - and 'callsInto': every call, including a method passed as a callable " +
                "reference such as 'repository::save', with its 'kind' (INTERNAL, PROJECT or " +
                "EXTERNAL), the line of the call itself, 'node' naming the id of the traced method it reaches " +
                "(null when it is not traced), and 'via' naming the interface method it is written against when " +
                "it reaches an implementation. 'chainLimitReached' is true when reachable methods were left out " +
                "because the chain hit its size limit of $MAX_TRACED_METHODS methods. " +
                "With includeTests, every node carries 'testReferences': the test files referring to the method, " +
                "or to the interface method named in 'via' - the tests a signature change will break - and the " +
                "starting method, when it handles an endpoint, also carries 'testUrlReferences': the test requests " +
                "whose URL matches it, with the matched 'endpointPath' - MockMvc to any host, since it never leaves " +
                "the JVM, WebTestClient, and java.net.http, RestTemplate, TestRestTemplate and RestClient to this " +
                "machine only; a request built inside a test helper from a parameter is not detected. A request " +
                "listed there is not repeated in 'testReferences'. Without includeTests both fields are absent, which " +
                "means not requested; an empty list means that no test was found. " +
                "A page holds at most 'limit' nodes ($TRACE_PAGE_LIMIT by default) within 'maxChars' of compact " +
                "JSON ($TRACE_PAGE_CHARS by default) and can end earlier, because the budget is measured on the " +
                "finished document. When 'truncated' is true, repeat the call with 'offset' = 'nextOffset' and " +
                "'expectedRevision' = 'revision': the pages together serve every node once, and a 'node' id on " +
                "one page refers to the same chain on another. A revision from a different query, or a project " +
                "changed since the first page, is answered with RESULT_CHANGED."
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
        @McpDescription(
            "How many layers deep to trace (default 3, at most 10). A layer is a call into another class; a call " +
                    "to the same class, its supertypes, its nested classes or a top-level function of the same " +
                    "file does not use one up."
        )
        depth: Int = 3,
        @McpDescription("Whether to find the test files that call a traced method (default true)")
        includeTests: Boolean = true,
        @McpDescription("Index of the first chain node to return; needs expectedRevision when above 0")
        offset: Int = 0,
        @McpDescription("Maximum chain nodes on this page, 1..50, $TRACE_PAGE_LIMIT by default")
        limit: Int = TRACE_PAGE_LIMIT,
        @McpDescription("Budget of the whole compact JSON answer, 512..16000, $TRACE_PAGE_CHARS by default")
        maxChars: Int = TRACE_PAGE_CHARS,
        @McpDescription("The 'revision' of the first page, required to continue that same answer")
        expectedRevision: String? = null,
    ): String {
        if (filePath.isBlank()) mcpFail("filePath must not be empty")
        if (line < 1) mcpFail("line must be >= 1")
        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
        val effectiveDepth = depth.coerceIn(1, 10)
        val page = PageRequest(offset = offset, limit = limit, maxChars = maxChars, expectedRevision = expectedRevision)

        return withContext(Dispatchers.IO) {
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
                val chain = CallChainTracer(project, MAX_TRACED_METHODS).trace(psiMethod, effectiveDepth)
                val tests = module?.takeIf { includeTests }?.let { CallChainTestReferences(it, project) }
                val revision = EntityInventory.revision(
                    project,
                    mapOf(
                        "filePath" to filePath,
                        "line" to line.toString(),
                        "depth" to effectiveDepth.toString(),
                        "includeTests" to includeTests.toString(),
                    )
                )

                BoundedPageWriter().write(
                    envelope = mapper.createObjectNode().put(FIELD_CHAIN_LIMIT_REACHED, chain.truncated),
                    itemsField = FIELD_CHAIN,
                    totalCount = chain.methods.size,
                    itemAt = { id ->
                        mapper.valueToTree(toCallChainNodeJson(id, chain.methods[id], chain, tests, project))
                    },
                    revision = revision,
                    page = page,
                )
            }
        }
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

    private fun toCallChainNodeJson(
        id: Int,
        traced: TracedMethod,
        chain: CallChain,
        tests: CallChainTestReferences?,
        project: Project,
    ): CallChainNodeJson {
        val method = traced.method
        val containingClass = method.containingClass
        val position = sourcePositionOf(method, project)
        val nodeTests = tests?.of(method, chain.viaDeclarationsOf(method), withUrlReferences = traced.reachedBy == null)
        return CallChainNodeJson(
            id = id,
            layer = containingClass?.let { detectSpringLayer(it) },
            reachedBy = traced.reachedBy?.name,
            className = containingClass?.qualifiedName ?: containingClass?.name,
            methodName = CallChainTracer.sourceNameOf(method),
            filePath = position.filePath,
            line = position.line,
            parameters = CallChainTracer.sourceParametersOf(method),
            aop = ProxyAnnotations.of(method).map { AopAnnotationJson(it.annotation, it.declaredOn.name) },
            callsInto = traced.calls.map { call ->
                CallTargetJson(
                    target = call.target,
                    kind = call.kind.name,
                    line = call.line,
                    node = call.reached?.let(chain::idOf),
                    via = call.via,
                )
            },
            testReferences = nodeTests?.references,
            testUrlReferences = nodeTests?.urlReferences,
        )
    }

    // ---- explyt_get_spring_data_entities ----

    @McpTool("explyt_get_spring_data_entities", title = "JPA entities with their table mapping")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Call before writing a query, a migration, a DTO or a projection, and before adding a field to " +
                "an entity, to see the table, column and relationship names the database actually uses. " +
                "A column name that differs from its field name, and a relationship's owning side, are exactly what " +
                "a query written from the Java field names gets wrong. " +
                "Answers in two steps: by default an inventory of the JPA entities (@Entity classes, " +
                "javax.persistence and jakarta.persistence alike) carrying only name, className, tableName and " +
                "source location - enough to pick one without expanding any schema. tableName is @Table(name) " +
                "when declared, otherwise the JPA default - the @Entity(name) entity name, then the class name - " +
                "before any physical naming strategy of the application is applied. " +
                "Pass includeDetails=true, and className to name the entity, to add its fields with column names, " +
                "types, primary key flag and nullability, its @OneToOne/@OneToMany/@ManyToOne/@ManyToMany " +
                "relationships with joinColumn/mappedBy, and the indexes declared in @Table(indexes=[...]). " +
                "An inventory record carries no 'fields' or 'indexes' at all, so a client never reads 'not " +
                "requested' as 'this entity has none'. " +
                "packageFilter narrows the inventory by prefix and className selects exactly one entity; passing " +
                "both is rejected, and a className nothing declares is an empty answer rather than an error. " +
                "Returns {status, revision, totalCount, offset, truncated, nextOffset, entities}: at most 'limit' " +
                "entities (5 by default) within 'maxChars' of compact JSON (1800 by default), and a page can end " +
                "earlier because the budget is measured on the finished document rather than on a number of " +
                "entities. When 'truncated' is true, repeat the call with 'offset' = 'nextOffset' and " +
                "'expectedRevision' = 'revision' to continue the same answer; every entity stays reachable that " +
                "way. A single entity too large for the budget is reported as RESPONSE_TOO_LARGE naming the " +
                "maxChars that would fetch it, never silently shortened."
    )
    suspend fun getSpringDataEntities(
        @McpDescription(PROJECT_PATH_DESCRIPTION)
        projectPath: String? = null,
        @McpDescription("Optional fully-qualified package prefix to restrict the inventory (e.g. 'com.example.domain'); cannot be combined with className")
        packageFilter: String = "",
        @McpDescription("Exact FQN of one entity, as reported by the inventory; cannot be combined with packageFilter")
        className: String? = null,
        @McpDescription("Adds fields, relationships and indexes to every entity of the page")
        includeDetails: Boolean = false,
        @McpDescription("Index of the first entity to return; needs expectedRevision when above 0")
        offset: Int = 0,
        @McpDescription("Maximum entities on this page, 1..50, 5 by default")
        limit: Int = 5,
        @McpDescription("Budget of the whole compact JSON answer, 512..16000, 1800 by default")
        maxChars: Int = 1800,
        @McpDescription("The 'revision' of the first page, required to continue that same answer")
        expectedRevision: String? = null,
    ): String {
        val writer = BoundedPageWriter()
        val packagePrefix = packageFilter.trim().takeIf { it.isNotEmpty() }
        val entityName = className?.trim()?.takeIf { it.isNotEmpty() }
        if (packagePrefix != null && entityName != null) {
            return writer.writeError(
                BoundedPageWriter.INVALID_ARGUMENT,
                "packageFilter and className narrow the same choice; pass one of them.",
                maxChars = BoundedPageWriter.FALLBACK_BUDGET
            )
        }

        val project = getCurrentProject(projectPath) ?: mcpFail(projectProblem(projectPath))
        val page = PageRequest(
            offset = offset,
            limit = limit,
            maxChars = maxChars,
            expectedRevision = expectedRevision
        )
        val inventory = EntityInventory(mapper)

        return withContext(Dispatchers.IO) {
            smartReadAction(project) {
                val records = inventory.ordered(entityRecords(project, packagePrefix, entityName))
                val revision = EntityInventory.revision(
                    project,
                    mapOf(
                        "packageFilter" to packagePrefix,
                        "className" to entityName,
                        "includeDetails" to includeDetails.toString()
                    )
                )
                writer.write(
                    envelope = mapper.createObjectNode(),
                    itemsField = "entities",
                    totalCount = records.size,
                    itemAt = { index ->
                        records[index].let { if (includeDetails) inventory.details(it) else inventory.compact(it) }
                    },
                    revision = revision,
                    page = page
                )
            }
        }
    }

    /**
     * Every entity the query selects, each able to read its own schema but none having read it yet.
     *
     * Expanding fields, relationships and indexes is the expensive half of this tool, and a page of ten names
     * must not pay for it across the whole project: the schema is read by [EntityInventory.details], for the
     * records of the requested page only, inside this same read action.
     */
    private fun entityRecords(project: Project, packagePrefix: String?, entityName: String?): List<EntityRecord> {
        val javaPsiFacade = JavaPsiFacade.getInstance(project)
        val librariesScope = GlobalSearchScope.allScope(project)
        val projectScope = project.projectScope()
        return ENTITY_ANNOTATION_FQNS.asSequence()
            .mapNotNull { javaPsiFacade.findClass(it, librariesScope) }
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, projectScope).findAll().asSequence() }
            .distinctBy { it.qualifiedName }
            .filter { cls ->
                val qualifiedName = cls.qualifiedName
                when {
                    entityName != null -> qualifiedName == entityName
                    packagePrefix != null -> qualifiedName?.startsWith(packagePrefix) == true
                    else -> true
                }
            }
            .mapNotNull { toEntityRecord(it, project) }
            .toList()
    }

    private fun toEntityRecord(psiClass: PsiClass, project: Project): EntityRecord? {
        val qualifiedName = psiClass.qualifiedName ?: return null
        val simpleName = psiClass.name ?: qualifiedName.substringAfterLast('.')
        val position = sourcePositionOf(psiClass, project)
        return EntityRecord(
            name = simpleName,
            className = qualifiedName,
            tableName = resolveTableName(psiClass, simpleName),
            filePath = position.filePath,
            line = position.line,
            readSchema = { EntitySchema(collectEntityFields(psiClass), collectEntityIndexes(psiClass)) }
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
        val anchor = McpSourcePositions.sourceAnchorOf(element) ?: return SourcePosition(null, null)
        return SourcePosition(relativePathOf(anchor, project), McpSourcePositions.lineOfAnchor(anchor))
    }

    /** 1-based line of [element], or `null` when it has no physical declaration to point at. */
    @VisibleForTesting
    internal fun lineOf(element: PsiElement): Int? =
        McpSourcePositions.sourceAnchorOf(element)?.let(McpSourcePositions::lineOfAnchor)

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
        private val mapper = ObjectMapper()

        // Match quality of an endpoint against the queried pattern, lowest first: the forgiving substring match
        // exists to find a route from a fragment, never to outrank the route that matches the pattern itself.
        private const val EXACT_MATCH = 0
        private const val PATTERN_MATCH = 1
        private const val SUBSTRING_MATCH = 2

        private const val COMPLETE_CONTRACT = "COMPLETE"
        private const val PARTIAL_CONTRACT = "PARTIAL"

        private val FRAMEWORK_PACKAGES = listOf("java.", "kotlin.", "org.springframework.")

        private const val TEMPLATE_NAME_GROUP = "name"

        // Enough to show the caller which line convention the file uses; the whole method list would bury it.
        private const val MAX_SUGGESTED_METHODS = 5

        // Guard cap for one trace: the project methods reachable from a handler within ten layers can be the whole
        // application, and a chain that long no longer tells the caller which path a change travels.
        private const val MAX_TRACED_METHODS = 50

        private const val TRACE_PAGE_LIMIT = 20
        private const val TRACE_PAGE_CHARS = 8000
        private const val FIELD_CHAIN = "chain"
        private const val FIELD_CHAIN_LIMIT_REACHED = "chainLimitReached"

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
            // Waits for this project's indexing instead of failing the whole call because some other one is dumb.
            smartReadAction(project) {
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
    @param:McpDescription("model that answered: STATIC or NATIVE_SNAPSHOT") val source: String,
    @param:McpDescription("loaded context the row comes from; absent for STATIC")
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val contextId: String?,
    @param:McpDescription("when the loaded context was last imported, ISO-8601 UTC; absent for STATIC or when unknown")
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val snapshotImportedAt: String?,
    @param:McpDescription("what the model cannot promise about this row's own bean; absent when nothing")
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val limitations: List<String>,
)

data class EndpointJson(
    val httpMethods: List<String>,
    val fullPath: String,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val pathTemplate: String?,
    val controllerClass: String?,
    val methodName: String?,
    val filePath: String?,
    /** `null` when the endpoint element has no physical declaration to point at. */
    val line: Int?,
    val parameters: List<EndpointParameterJson>,
    val returnType: String?,
    val endpointType: String,
    /** The media types the mapping accepts, present only when it declares some; see [CompactEndpointJson.consumes]. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val consumes: List<String>,
    /** The media types the mapping produces, present only when it declares some. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val produces: List<String>,
    /** Whether an Actuator endpoint answers over HTTP; see [CompactEndpointJson.exposed]. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val exposed: String?,
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
    /** The path the application serves, configuration placeholders resolved. */
    val fullPath: String,
    /**
     * The path as declared, placeholders included - present only when it differs from [fullPath], so the common
     * endpoint keeps its shape. A profile or an environment variable can override the resolved value at runtime.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val pathTemplate: String?,
    val controllerClass: String?,
    val methodName: String?,
    val filePath: String?,
    /** `null` when the endpoint element has no physical declaration to point at. */
    val line: Int?,
    val endpointType: String,
    /**
     * The `Content-Type` values the mapping accepts. Two handlers sharing a path and a verb are told apart by these
     * and [produces], not by their order. Absent rather than empty when the mapping declares none - any media type is
     * accepted then - so the common endpoint keeps its shape.
     */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val consumes: List<String>,
    /** The media types the mapping produces, matched against the request's `Accept`; absent when it declares none. */
    @get:JsonInclude(JsonInclude.Include.NON_EMPTY) val produces: List<String>,
    /**
     * For an Actuator endpoint only, whether `management.endpoints.web.exposure.include`/`exclude` let it answer over
     * HTTP: `EXPOSED`, `NOT_EXPOSED`, or `UNKNOWN` when a value cannot be read from the configuration files. Absent for
     * every other endpoint. A profile or an environment variable can change exposure at run time, so an endpoint is
     * listed whatever this says.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val exposed: String?,
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
 * [endpoints] lists the closest match first, and among equally close ones the more specific path first, the way
 * Spring ranks path patterns, so of routes with different paths its first element is the one that dispatches.
 * Handlers sharing one path are chosen by the request's media types instead, which their `consumes` and `produces`
 * show; their order says nothing about dispatch. [basePath] is the leading path
 * stripped from the URL because the configuration of the answering routes' module declares it - a servlet context
 * path, a dispatcher servlet path or a WebFlux base path - a fact, not a guess. [assumedPrefix] is a leading path
 * dropped from the URL although nothing declares it - a context path living only in deployment configuration or a
 * gateway prefix - a guess. Both are null when the URL matched as written, and at most one of them is set.
 * [nearestByPrefix] is filled only when [endpoints] is empty: the routes
 * sharing the longest leading path ([sharedPrefix]) with the URL, which is where a route that does not exist yet
 * would be added. The two lists are empty rather than null so a client can always iterate them; [sharedPrefix] is
 * null exactly when [nearestByPrefix] is empty.
 */
data class EndpointLookupJson<T>(
    val totalCount: Int,
    val truncated: Boolean,
    val endpoints: List<T>,
    val basePath: String?,
    val assumedPrefix: String?,
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
    /**
     * Whether Spring rejects a request that omits the value. A query parameter or a header with a `defaultValue`,
     * or one declared `Optional`, `@Nullable`, Kotlin-nullable or with a Kotlin default, is `false` whatever its
     * `required` attribute says. `null` when nothing declares it, which is every source the tool cannot read the
     * contract of.
     */
    val required: Boolean?,
    val defaultValue: String? = null,
)


data class CallChainNodeJson(
    /** Position of the node in the chain, which [CallTargetJson.node] refers to. */
    val id: Int,
    /** Stereotype of the declaring class; `null` when that class is not a Spring bean, e.g. a Kotlin `object`. */
    val layer: String?,
    /**
     * `INTERNAL` for a helper of the unit of code that called it, `PROJECT` for a method of another class, `null` for
     * the method the trace started from.
     */
    val reachedBy: String?,
    val className: String?,
    val methodName: String,
    val filePath: String?,
    /** `null` for a light or synthetic method with no physical declaration, e.g. a generated `copy()`. */
    val line: Int?,
    val parameters: List<String>,
    /**
     * The proxy annotations Spring applies around the method - `@Transactional`, `@Async`, the cache annotations -
     * declared on the method or on its class, the method's own first. Empty when none is declared.
     */
    val aop: List<AopAnnotationJson>,
    val callsInto: List<CallTargetJson>,
    /**
     * The test code referring to this method, or to the interface method a call reached it through. Absent when
     * tests were not requested, empty when none refers to it.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val testReferences: List<NodeTestReferenceJson>? = null,
    /**
     * On the method the trace started from, when it handles an endpoint: the test requests whose URL matches the
     * endpoint - MockMvc to any host, `WebTestClient`, and real HTTP clients to this machine. Absent on every other node
     * and when tests were not requested.
     */
    @get:JsonInclude(JsonInclude.Include.NON_NULL)
    val testUrlReferences: List<UrlTestReferenceJson>? = null,
)

data class AopAnnotationJson(
    /** Fully qualified name of the Spring or JTA annotation, also when a project annotation carries it as a meta-annotation. */
    val annotation: String,
    /** `METHOD` or `CLASS`. */
    val declaredOn: String,
)

data class NodeTestReferenceJson(
    val filePath: String,
    val lines: List<Int>,
    /** The interface or abstract method the test refers to instead of this method, `null` for a direct reference. */
    val via: String?,
)

data class UrlTestReferenceJson(
    val filePath: String,
    val lines: List<Int>,
    /**
     * The mapping path of the endpoint the test's request matched, as the endpoint declares it - not the literal URL
     * the test sends, which may carry concrete values in place of `{templates}`.
     */
    val endpointPath: String,
)

data class CallTargetJson(
    val target: String,
    /**
     * `INTERNAL` for a helper of the caller's own unit of code, `PROJECT` for a method of another project class, and
     * `EXTERNAL` where the request leaves the application: a library method called on an injected dependency, or a
     * method the framework implements, such as a Spring Data repository method.
     */
    val kind: String,
    /** Line of the call in the calling method, where a change to its arguments is made. */
    val line: Int?,
    /** Id of the chain node the call reaches; `null` for an `EXTERNAL` call or a method the trace did not expand. */
    val node: Int?,
    /** The interface or abstract method the call is written against, when [target] is an implementation of it. */
    val via: String?,
)

data class ServiceCallJson(
    val target: String,
    /** Where [target] is declared. */
    val filePath: String?,
    /** Declaration line of [target]. */
    val line: Int?,
    /** Line of the call in the handler; absent for a functional route, whose handler is a reference, not a call. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val callLine: Int? = null,
)


data class EndpointContractJson(
    val httpMethods: List<String>,
    val fullPath: String,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val pathTemplate: String?,
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
    /** The first of [serviceCalls]; kept for callers of the generation that had only it. */
    val serviceCall: ServiceCallJson?,
    /**
     * Every call on an injected project bean in source order, or the handler function of a functional route. Empty when
     * the handler calls no such bean.
     */
    val serviceCalls: List<ServiceCallJson>,
    val endpointType: String,
    /** Whether an Actuator endpoint answers over HTTP; see [CompactEndpointJson.exposed]. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val exposed: String?,
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

/**
 * The shape of a value as Jackson writes it.
 *
 * An enum has no [fields]: it carries [enumValues] - the strings written for its constants - or, when it declares a
 * `@JsonValue` member, [jsonValue] naming that member and [valueType] its type. The values of a `@JsonValue` member
 * are computed in code, so they are not listed; [enumConstants] lists the constants themselves instead.
 *
 * @property enumConstants the declared constant names of a `@JsonValue` enum - NOT the values written on the wire -
 * so a caller knows how many values there are and which constant each comes from. Absent for an enum without
 * `@JsonValue`, whose [enumValues] already are its wire values and would only be repeated or contradicted.
 */
data class DtoSchemaJson(
    val className: String,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val fields: List<DtoFieldJson>?,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val enumValues: List<String>? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val jsonValue: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val valueType: String? = null,
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val enumConstants: List<String>? = null,
)

data class DtoFieldJson(
    /** The name the field is written under: its `@JsonProperty` value when it has one. */
    val name: String,
    /** The name the field is declared with, present only when `@JsonProperty` renames it. */
    @get:JsonInclude(JsonInclude.Include.NON_NULL) val declaredName: String?,
    val type: String,
    val nullable: Boolean,
    val nested: DtoSchemaJson?,
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

