/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.contract

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.source.ProjectSources
import com.explyt.spring.core.source.SourceLocation
import com.explyt.spring.core.source.SourcePosition
import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.trace.CallChainTracer
import com.explyt.spring.web.trace.CallSite
import com.explyt.spring.web.trace.FunctionalRouteTarget
import com.explyt.spring.web.trace.HttpMethods
import com.explyt.spring.web.trace.InjectedDependencies
import com.explyt.spring.web.trace.MethodCallSite
import com.explyt.spring.web.trace.SourceNames
import com.explyt.spring.web.trace.UnresolvedCallSite
import com.explyt.spring.web.trace.methodKey
import com.explyt.spring.web.util.HandlerMethods
import com.explyt.spring.web.util.HandlerSignature
import com.explyt.spring.web.util.SpringWebUtil
import com.explyt.spring.web.util.WebApplicationStack
import com.explyt.util.ExplytAnnotationUtil.getBooleanAttribute
import com.explyt.util.ExplytAnnotationUtil.getStringAttribute
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedByOrSelf
import com.intellij.codeInspection.isInheritorOf
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.CommonClassNames
import com.intellij.psi.PsiAnnotation
import com.intellij.psi.PsiArrayType
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.PsiField
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.PsiParameter
import com.intellij.psi.PsiPrimitiveType
import com.intellij.psi.PsiType
import com.intellij.psi.util.InheritanceUtil
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UCallableReferenceExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.UParameter
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement
import org.jetbrains.uast.visitor.AbstractUastVisitor

object EndpointContracts {

    /**
     * The contract of [endpoint], complete when a request-handling method declares it and partial otherwise.
     *
     * A partial contract is deliberately still a contract: dropping the endpoint instead would leave the caller
     * with a `totalCount` naming an endpoint it cannot see, which reads as "the route does not exist" - the
     * opposite of what the endpoint model found. What is knowable without a handler signature - the path, the
     * verb, the declaring bean factory, and the parameters the URL template itself declares - is reported, and
     * [EndpointContract.unavailableReason] names what is missing rather than leaving the caller to
     * infer it from empty fields.
     */
    fun contractOf(endpoint: EndpointElement, project: Project): EndpointContract {
        val handler = requestHandlerOf(endpoint)
        val uHandler = handler?.toUElement() as? UMethod
        val module = ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement)
        val summary = summaryOf(endpoint, project)

        val serviceCalls = when {
            handler != null && uHandler != null && module != null ->
                injectedBeanCallsOf(handler, uHandler, module, project)
            else -> listOfNotNull(routeHandlerCall(endpoint, project))
        }

        return EndpointContract(
            endpoint = summary,
            parameters = parametersOf(endpoint, handler),
            returnType = handler?.let(HandlerSignature::declaredReturnType)?.canonicalText,
            responseSchema = handler?.let(HandlerSignature::declaredReturnType)
                ?.let { ResponseSchemaReader.schemaOf(it, depth = 3, module = module) },
            serviceCalls = serviceCalls,
            status = if (handler != null) ContractStatus.COMPLETE else ContractStatus.PARTIAL,
            unavailableReason = if (handler != null) null else contractUnavailableReason(endpoint),
        )
    }

    fun summaryOf(endpoint: EndpointElement, project: Project): EndpointSummary {
        val declaringMethod = declaringMethodOf(endpoint)
        val controllerClass = endpoint.containingClass ?: declaringMethod?.containingClass
        val position = SourcePosition.of(endpoint.psiElement, project)
        // A loader may know the declaring file of an endpoint whose element has no source position of its own.
        val location = position.location.takeIf { it != SourceLocation.UNKNOWN }
            ?: endpoint.containingFile?.let { SourceLocation.of(it, project) }
            ?: SourceLocation.UNKNOWN
        val mediaTypes = mediaTypesOf(
            requestHandlerOf(endpoint)?.toUElement() as? UMethod,
            ModuleUtilCore.findModuleForPsiElement(endpoint.psiElement),
            project,
        )

        return EndpointSummary(
            httpMethods = HttpMethods.listed(endpoint.requestMethods),
            fullPath = endpoint.path,
            pathTemplate = endpoint.pathTemplate,
            controllerClass = controllerClass?.qualifiedName,
            methodName = declaringMethod?.name,
            position = SourcePosition(location, position.line),
            type = endpoint.type,
            consumes = mediaTypes.consumes,
            produces = (endpoint.produces + mediaTypes.produces).distinct(),
            producesSource = endpoint.producesSource,
            exposure = endpoint.exposure,
            access = endpoint.access,
            beanCondition = endpoint.beanCondition,
            testSource = declaredInTests(endpoint),
            application = endpoint.application?.qualifiedName,
        )
    }

    fun parametersOf(endpoint: EndpointElement): List<EndpointParameter> =
        parametersOf(endpoint, requestHandlerOf(endpoint))

    fun returnTypeOf(endpoint: EndpointElement): String? =
        requestHandlerOf(endpoint)?.let(HandlerSignature::declaredReturnType)?.canonicalText

    /**
     * Whether [endpoint] belongs to test code: decided by the controller serving it, not by the method declaring
     * the mapping - a test-source probe inheriting a production base class's `@GetMapping` is still test code.
     */
    fun declaredInTests(endpoint: EndpointElement): Boolean =
        ProjectSources.declaresInTests(endpoint.containingClass ?: endpoint.psiElement) ||
                endpoint.application?.let(ProjectSources::declaresInTests) == true

    /**
     * The method whose declaration carries [endpoint], handler or not.
     *
     * A functional route is a call inside a `@Bean` factory rather than a member of its own, so the element the
     * loader reports is an expression. Naming the enclosing factory is what makes such a route reachable: its path
     * appears nowhere else in the project, and without the method a caller is left with a file and a line.
     */
    fun declaringMethodOf(endpoint: EndpointElement): PsiMethod? =
        endpoint.psiElement as? PsiMethod
            ?: endpoint.psiElement.toUElement()?.getParentOfType<UMethod>()?.javaPsi

    private fun parametersOf(endpoint: EndpointElement, handler: PsiMethod?): List<EndpointParameter> = when {
        handler == null -> pathTemplateParameters(endpoint.path)
        endpoint.type == EndpointType.ACTUATOR && isActuatorOperation(handler) -> actuatorOperationParameters(handler)
        else -> extractParameters(handler)
    }

    private fun isActuatorOperation(method: PsiMethod): Boolean =
        ACTUATOR_OPERATION_SOURCES.keys.any { method.isMetaAnnotatedBy(it) }

    /**
     * The parameters of an Actuator operation, bound the way Boot's web operation mapping binds them: a `@Selector`
     * is a path segment, the arguments of a `@WriteOperation` are fields of the JSON request body, and those of a read
     * or delete operation are query parameters. A parameter Boot supplies itself - the security context, the server
     * namespace, a producible media type, the API version - is not part of the request.
     *
     * An argument is mandatory unless it is nullable, which is Boot's `OperationMethodParameter.isMandatory`.
     */
    private fun actuatorOperationParameters(method: PsiMethod): List<EndpointParameter> {
        val argumentSource = ACTUATOR_OPERATION_SOURCES.entries.first { method.isMetaAnnotatedBy(it.key) }.value
        return method.parameterList.parameters.map { param ->
            val source = when {
                param.isMetaAnnotatedBy(SpringCoreClasses.ACTUATOR_SELECTOR) -> "PATH"
                ACTUATOR_SUPPLIED_TYPES.any { InheritanceUtil.isInheritor(param.type, it) } -> "FRAMEWORK"
                else -> argumentSource
            }
            val required = if (source == "FRAMEWORK") null else !isNullableArgument(param)
            EndpointParameter(param.name, source, param.type.canonicalText, required, declaration = param)
        }
    }

    /**
     * Boot 4 reads nullness through `Nullness.forParameter`, which also sees JSpecify's type-use `@Nullable` - the one
     * its own operations use, carried on the parameter's type rather than on its modifier list.
     */
    private fun isNullableArgument(param: PsiParameter): Boolean =
        with(SpringWebUtil) { param.acceptsMissingValue() } ||
                param.type.annotations.any { it.qualifiedName?.substringAfterLast('.') == NULLABLE_SIMPLE_NAME }

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
    private fun pathTemplateParameters(path: String): List<EndpointParameter> =
        SpringWebUtil.NameInBracketsRx.findAll(path)
            .mapNotNull { it.groups[TEMPLATE_NAME_GROUP]?.value?.substringBefore(':')?.takeIf(String::isNotBlank) }
            .distinct()
            .map { EndpointParameter(it, "PATH", CommonClassNames.JAVA_LANG_STRING, required = true) }
            .toList()

    private fun extractParameters(psiMethod: PsiMethod): List<EndpointParameter> {
        val result = mutableListOf<EndpointParameter>()

        for (info in SpringWebUtil.collectPathVariables(psiMethod)) {
            result += EndpointParameter(
                info.name, "PATH", declaredTypeOf(info), info.isRequired, declaration = info.psiElement
            )
        }
        val multipartRequestNames = HandlerSignature.requestParameters(psiMethod).asSequence()
            .filter { inherited(it, SpringWebClasses.REQUEST_PARAM) != null && isMultipartPart(it.type) }
            .map(::wireNameOf)
            .toSet()
        val stack = webStackOf(psiMethod)
        val servletApplication = stack == WebApplicationStack.SERVLET
        for (info in SpringWebUtil.collectRequestParameters(psiMethod)) {
            val source = if (servletApplication && info.name in multipartRequestNames) "PART" else "QUERY"
            result += EndpointParameter(
                info.name, source, declaredTypeOf(info), info.isRequired, info.defaultValue, info.psiElement
            )
        }
        for (param in HandlerSignature.requestParameters(psiMethod)) {
            val part = inherited(param, SpringWebClasses.REQUEST_PART) ?: continue
            result += EndpointParameter(
                name = wireNameOf(param),
                source = "PART",
                type = param.type.canonicalText,
                required = part.getBooleanAttribute("required") ?: true,
                declaration = param,
            )
        }
        val body = SpringWebUtil.getRequestBodyInfo(psiMethod)
        if (body != null) {
            result += EndpointParameter(
                body.name, "BODY", declaredTypeOf(body), body.isRequired, declaration = body.psiElement
            )
        }
        for (info in SpringWebUtil.collectRequestHeaders(psiMethod)) {
            result += EndpointParameter(
                info.name, "HEADER", declaredTypeOf(info), info.isRequired, info.defaultValue, info.psiElement
            )
        }

        result += parametersOutsideCollectors(psiMethod, stack)
        return result
    }

    /**
     * The type the handler declares. The collectors box a primitive for the OpenAPI and HTTP-client consumers, which
     * need a reference type, but `int` and `java.lang.Integer` differ for a client: only the latter may be absent.
     */
    private fun declaredTypeOf(info: SpringWebUtil.PathArgumentInfo): String {
        val parameter = info.psiElement as? PsiParameter
            ?: info.psiElement.toUElement()?.getParentOfType(UParameter::class.java, false)?.javaPsi as? PsiParameter
        val type = parameter?.type
        return if (type is PsiPrimitiveType) type.canonicalText else info.typeFqn
    }

    private fun webStackOf(psiMethod: PsiMethod): WebApplicationStack? {
        val module = ModuleUtilCore.findModuleForPsiElement(psiMethod) ?: return null
        return WebApplicationStack.of(module)
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
     * contract to audit it. Reporting the parameter - with Spring's default source, or `UNKNOWN` when even that
     * cannot be told - says "there is an argument here", which is actionable; silence is not.
     */
    private fun parametersOutsideCollectors(
        psiMethod: PsiMethod,
        stack: WebApplicationStack?,
    ): List<EndpointParameter> =
        HandlerSignature.requestParameters(psiMethod)
            .filter { param ->
                COLLECTED_BINDING_ANNOTATIONS.none { binding ->
                    HandlerMethods.annotatedParameter(param) { it.isMetaAnnotatedBy(binding) } != null
                }
            }
            .map { param ->
                EndpointParameter(
                    name = wireNameOf(param),
                    source = sourceOfUncollected(param, stack),
                    type = param.type.canonicalText,
                    // Not null-by-omission: the collected annotations declare requiredness, whereas a
                    // resolver's contract is private to the resolver. Defaulting to `true` or `false` here would
                    // invent a fact about the wire format, which is the failure this method exists to avoid.
                    required = null,
                    declaration = param,
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
    private fun inherited(param: PsiParameter, annotationFqn: String): PsiAnnotation? =
        HandlerMethods.bindingAnnotationOf(param) { it.isMetaAnnotatedByOrSelf(annotationFqn) }

    private fun wireNameOf(param: PsiParameter): String {
        val annotation = NAMED_BINDING_ANNOTATIONS.firstNotNullOfOrNull { inherited(param, it) } ?: return param.name
        return annotation.getStringAttribute("value")
            ?: annotation.getStringAttribute(ATTR_NAME)
            ?: param.name
    }

    private fun sourceOfUncollected(param: PsiParameter, stack: WebApplicationStack?): String {
        val annotated = HandlerMethods.bindingAnnotationOf(param) { it.isRuntimeParameterAnnotation() } != null
        val frameworkSource = FrameworkArguments.sourceOf(param.type, stack, annotated)
        return when {
            inherited(param, SpringWebClasses.COOKIE_VALUE) != null -> "COOKIE"
            inherited(param, SpringWebClasses.MODEL_ATTRIBUTE) != null -> "MODEL"
            frameworkSource != null -> frameworkSource.name
            HandlerMethods.bindingAnnotationOf(param) { !isBindingNeutral(it) } != null -> "UNKNOWN"
            else -> defaultSourceOf(param.type, stack == WebApplicationStack.SERVLET)
        }
    }

    /**
     * Where Spring's catch-all resolvers put a parameter no annotation claims - the tail of the resolver chain of
     * both servlet MVC and WebFlux: `RequestParamMethodArgumentResolver(useDefaultResolution = true)` for a simple
     * type, then the model-attribute resolver (`annotationNotRequired = true`) for any other.
     *
     * The types Spring claims ahead of that tail without an annotation keep their own source: a multipart part is a
     * request part on servlet MVC, and `HttpEntity`, `Pageable` and `Sort` stay
     * `UNKNOWN` because their resolvers read a wire format of their own. A type that does not resolve stays
     * `UNKNOWN` too: whether it is simple is the question that cannot be answered.
     */
    private fun defaultSourceOf(type: PsiType, servletApplication: Boolean): String {
        val value = optionalValueOf(type)
        return when {
            !isResolved(value) -> "UNKNOWN"
            OWN_WIRE_FORMAT_TYPES.any { InheritanceUtil.isInheritor(value, it) } -> "UNKNOWN"
            isMultipartPart(value) -> if (servletApplication) "PART" else "UNKNOWN"
            SpringSimpleValueTypes.isSimpleProperty(value) -> "QUERY"
            else -> "MODEL"
        }
    }

    /** `Optional<T>` is read as `T`, as `MethodParameter.nestedIfOptional` does for a request parameter. */
    private fun optionalValueOf(type: PsiType): PsiType {
        val classType = type as? PsiClassType ?: return type
        if (classType.resolve()?.qualifiedName != CommonClassNames.JAVA_UTIL_OPTIONAL) return type
        return classType.parameters.singleOrNull() ?: type
    }

    private fun isResolved(type: PsiType): Boolean = when (type) {
        is PsiArrayType -> isResolved(type.componentType)
        is PsiClassType -> type.resolve() != null
        else -> true
    }

    /**
     * An annotation that leaves the parameter to Spring's default binding: validation, formatting and nullability
     * annotations do not claim a parameter. Any other one may be the key of a custom argument resolver.
     */
    private fun isBindingNeutral(annotation: PsiAnnotation): Boolean {
        val qualifiedName = annotation.qualifiedName ?: return false
        return BINDING_NEUTRAL_ANNOTATION_PACKAGES.any { qualifiedName.startsWith(it) } ||
                qualifiedName.substringAfterLast('.') in BINDING_NEUTRAL_ANNOTATION_NAMES
    }

    private data class MediaTypes(val produces: List<String>, val consumes: List<String>)

    private fun mediaTypesOf(
        uHandler: UMethod?,
        module: Module?,
        project: Project,
    ): MediaTypes {
        if (uHandler == null || module == null) return MediaTypes(emptyList(), emptyList())
        val mah = SpringSearchService.getInstance(project)
            .getMetaAnnotations(module, SpringWebClasses.REQUEST_MAPPING)
        val mappingSource = HandlerMethods.mappingSourceOf(uHandler.javaPsi) { it.isMetaAnnotatedBy(SpringWebClasses.REQUEST_MAPPING) }
            ?: uHandler.javaPsi
        val mapping = HandlerMethods.methodMapping(mappingSource, mah)
        return MediaTypes(produces = mapping.produces, consumes = mapping.consumes)
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
    private fun routeHandlerCall(endpoint: EndpointElement, project: Project): ServiceCall? {
        val callee = (FunctionalRouteTarget.of(endpoint.psiElement) as? FunctionalRouteTarget.Handler)?.method
            ?: return null
        val position = SourcePosition.of(callee, project)
        val target = SourceNames.qualifiedNameOf(callee) ?: return null
        return ServiceCall(target = target, position = position, callee = callee)
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
        module: Module,
        project: Project,
    ): List<ServiceCall> {
        val controllerClass = psiMethod.containingClass ?: return emptyList()
        val beanClasses = SpringSearchService.getInstance(project).getProjectBeans(module).map { it.psiClass }
        val beanFields = controllerClass.allFields.filter { field ->
            if (!InjectedDependencies.isInjected(field)) return@filter false
            val fieldClass = (field.type as? PsiClassType)?.resolve() ?: return@filter false
            beanClasses.any { InheritanceUtil.isInheritorOrSelf(it, fieldClass, true) }
        }.toSet()
        if (beanFields.isEmpty()) return emptyList()

        val calls = mutableListOf<ServiceCall>()
        val visited = mutableSetOf<String>()
        fun visit(method: PsiMethod, body: UMethod) {
            if (!visited.add(methodKey(method))) return
            fun collect(site: CallSite?) {
                when (site) {
                    null -> Unit
                    is UnresolvedCallSite -> unresolvedServiceCallOf(site, controllerClass, beanFields)?.let { calls += it }
                    is MethodCallSite -> {
                        val call = serviceCallOf(site, controllerClass, beanFields, project)
                        if (call != null) calls += call
                        else ownHelperOf(site, psiMethod)?.let { (helper, helperBody) -> visit(helper, helperBody) }
                    }
                }
            }
            body.accept(object : AbstractUastVisitor() {
                override fun visitCallExpression(node: UCallExpression): Boolean {
                    ProgressManager.checkCanceled()
                    collect(CallSite.of(node))
                    return false
                }

                override fun visitCallableReferenceExpression(node: UCallableReferenceExpression): Boolean {
                    collect(CallSite.of(node))
                    return false
                }
            })
        }
        visit(psiMethod, uMethod)
        return calls.distinctBy { it.target to it.callLine }
    }

    private fun ownHelperOf(site: MethodCallSite, handler: PsiMethod): Pair<PsiMethod, UMethod>? {
        val helper = site.callee
        if (site.receiver != null && !site.isOnSelf) return null
        if (helper.isConstructor || helper.hasModifierProperty(PsiModifier.ABSTRACT)) return null
        if (!ProjectSources.declares(helper) || !CallChainTracer.isInternal(handler, helper)) return null
        val body = (helper.navigationElement.toUElement() ?: helper.toUElement()) as? UMethod ?: return null
        return helper to body
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

    private fun unresolvedServiceCallOf(
        site: UnresolvedCallSite,
        controllerClass: PsiClass,
        beanFields: Set<PsiField>,
    ): ServiceCall? {
        val field = InjectedDependencies.fieldOf(site.receiver, controllerClass)?.takeIf { it in beanFields } ?: return null
        return ServiceCall(
            target = "${InjectedDependencies.declaredTypeNameOf(field)}.${site.methodName}",
            position = SourcePosition.UNKNOWN,
            callLine = site.line,
            resolved = false,
        )
    }

    private fun isFrameworkInterfaceMemberOfProjectType(calleeClass: PsiClass, receiverClass: PsiClass): Boolean {
        val fqn = calleeClass.qualifiedName ?: return false
        return calleeClass.isInterface
                && PLATFORM_PACKAGES.none(fqn::startsWith)
                && ProjectSources.declares(receiverClass)
    }

    /** The service call [site] makes, when it invokes a project method of one of the controller's injected beans. */
    private fun serviceCallOf(
        site: MethodCallSite,
        controllerClass: PsiClass,
        beanFields: Set<PsiField>,
        project: Project,
    ): ServiceCall? {
        val field = InjectedDependencies.fieldOf(site.receiver, controllerClass)?.takeIf { it in beanFields } ?: return null
        val callee = site.callee
        val receiverClass = (field.type as? PsiClassType)?.resolve() ?: return null
        val calleeClass = callee.containingClass ?: return null
        if (callee.isConstructor
            || callee.hasModifierProperty(PsiModifier.STATIC)
            || !InheritanceUtil.isInheritorOrSelf(receiverClass, calleeClass, true)
        ) return null
        val namedAfter = when {
            !isPlatformOrFrameworkLibraryMember(calleeClass) -> null
            isFrameworkInterfaceMemberOfProjectType(calleeClass, receiverClass) -> receiverClass
            else -> return null
        }
        val target = SourceNames.qualifiedNameOf(callee, namedAfter) ?: return null
        return ServiceCall(
            target = target, position = SourcePosition.of(callee, project), callLine = site.line, callee = callee
        )
    }

    private val PLATFORM_PACKAGES = listOf("java.", "kotlin.")
    private val FRAMEWORK_PACKAGES = PLATFORM_PACKAGES + "org.springframework."

    private const val TEMPLATE_NAME_GROUP = "name"
    private const val ATTR_NAME = "name"
    private const val NULLABLE_SIMPLE_NAME = "Nullable"

    /** Return types of a route-registration bean, whose signature describes the registration, not a request. */
    private val ROUTE_FUNCTION_TYPES = listOf(
        SpringWebClasses.ROUTE_FUNCTION,
        SpringWebClasses.SERVLET_ROUTE_FUNCTION,
    )

    private val ACTUATOR_OPERATION_SOURCES = linkedMapOf(
        SpringCoreClasses.ACTUATOR_WRITE_OPERATION to "BODY",
        SpringCoreClasses.ACTUATOR_READ_OPERATION to "QUERY",
        SpringCoreClasses.ACTUATOR_DELETE_OPERATION to "QUERY",
    )

    private val ACTUATOR_SUPPLIED_TYPES = listOf(
        "org.springframework.boot.actuate.endpoint.SecurityContext",
        "org.springframework.boot.actuate.endpoint.web.WebServerNamespace",
        "org.springframework.boot.actuate.endpoint.Producible",
        "org.springframework.boot.actuate.endpoint.ApiVersion",
        "java.security.Principal",
    )

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

    /** Resolved by Spring without an annotation, from a wire format of their own: the body or `?page=&sort=`. */
    private val OWN_WIRE_FORMAT_TYPES = listOf(
        "org.springframework.http.HttpEntity",
        "org.springframework.data.domain.Pageable",
        "org.springframework.data.domain.Sort",
    )

    private val BINDING_NEUTRAL_ANNOTATION_PACKAGES = listOf(
        "jakarta.validation.",
        "javax.validation.",
        "org.springframework.validation.annotation.",
        "org.springframework.format.annotation.",
        "org.jetbrains.annotations.",
        "kotlin.",
    )

    /** Matched by simple name too, so an annotation whose library is not on the classpath still reads as neutral. */
    private val BINDING_NEUTRAL_ANNOTATION_NAMES = setOf(
        "Valid", "Validated", "DateTimeFormat", "NumberFormat", "NotNull", "NonNull", "Nonnull", "Nullable",
    )
}
