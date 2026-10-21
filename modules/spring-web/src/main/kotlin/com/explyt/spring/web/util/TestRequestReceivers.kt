/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.web.SpringWebClasses
import com.explyt.spring.web.util.EndpointUrlMatcher.Policy
import com.intellij.psi.PsiMethod
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UExpression
import org.jetbrains.uast.UQualifiedReferenceExpression

/**
 * The real HTTP clients a test sends a request with, and how each of them states the request's URL and method.
 *
 * A real client goes over the network, so only a request to this machine - a `RANDOM_PORT` test calling
 * `http://localhost:$port/...` - addresses the project ([Policy.REFERENCE]); a request to another host is a call to
 * another service. MockMvc never leaves the JVM and is searched with [Policy.IN_PROCESS] by its own code.
 *
 * Only the URL argument of a listed call is read. A string that merely looks like a path - test data handed to a
 * classifier, a fixture of expected values - is not a request and is never matched.
 */
object TestRequestReceivers {

    /** Where a client call states the HTTP method of its request. */
    sealed interface Verb {
        /** The method name fixes it: `getForObject` sends a GET. */
        data class Fixed(val name: String) : Verb

        /** An `org.springframework.http.HttpMethod` argument of the same call: `exchange(url, HttpMethod.GET, ...)`. */
        data object HttpMethodArgument : Verb

        /** The call the URL call is made on: `restClient.get().uri(...)`, `restClient.method(HttpMethod.PUT).uri(...)`. */
        data object ReceiverCall : Verb

        /**
         * A call of the same builder chain: `HttpRequest.newBuilder(uri).POST(body).build()`. A chain built to the end
         * without naming a method sends a GET, the builder's default; a chain left unfinished says nothing.
         */
        data object BuilderChain : Verb
    }

    /** A call that takes the URL of a request: [method] declared by [owner], the URL being a `String` or `URI`. */
    data class Receiver(val owner: String, val method: String, val verb: Verb)

    /** RestOperations calls whose name fixes the HTTP method; declared before [NETWORK], which reads it. */
    private val FIXED_VERBS = listOf(
        "getForObject" to "GET", "getForEntity" to "GET", "headForHeaders" to "HEAD",
        "postForObject" to "POST", "postForEntity" to "POST", "postForLocation" to "POST",
        "put" to "PUT", "patchForObject" to "PATCH", "delete" to "DELETE", "optionsForAllow" to "OPTIONS",
    )

    val NETWORK: List<Receiver> = listOf(
        Receiver(SpringWebClasses.JAVA_HTTP_REQUEST, "newBuilder", Verb.BuilderChain),
        Receiver(SpringWebClasses.JAVA_HTTP_REQUEST_BUILDER, "uri", Verb.BuilderChain),
        Receiver(SpringWebClasses.REST_CLIENT_URI_SPEC, "uri", Verb.ReceiverCall),
    ) + listOf(SpringWebClasses.REST_OPERATIONS, SpringWebClasses.REST_TEMPLATE, SpringWebClasses.TEST_REST_TEMPLATE)
        .flatMap(::restOperationsOf)

    /**
     * The index of the request URL among the parameters of [method] - a template, a `java.net.URI` or a
     * `Function<UriBuilder, URI>` - or `-1` when it takes none.
     */
    fun urlParameterIndex(method: PsiMethod): Int =
        SpringWebUtil.getUrlTemplateIndex(method).takeIf { it != -1 }
            ?: method.parameterList.parameters.indexOfFirst { it.type.canonicalText in URL_TYPES }.takeIf { it != -1 }
            ?: UrlArgumentText.uriBuilderFunctionParameterIndex(method)

    /** The HTTP method [call] sends, upper-case, or `null` when it is not stated where [verb] says to look. */
    fun httpMethodOf(call: UCallExpression, method: PsiMethod, verb: Verb): String? = when (verb) {
        is Verb.Fixed -> verb.name
        Verb.HttpMethodArgument -> httpMethodArgumentOf(call, method)
        Verb.ReceiverCall -> callOf(call.receiver)?.let(::verbNamedBy)
        Verb.BuilderChain -> {
            val chain = chainOf(call)
            chain.firstNotNullOfOrNull { if (it === call) null else verbNamedBy(it) }
                ?: GET.takeIf { chain.any { it.methodName == BUILD } }
        }
    }

    private fun verbNamedBy(call: UCallExpression): String? {
        val name = call.methodName ?: return null
        if (name == METHOD) return call.valueArguments.firstOrNull()?.let(::httpMethodNameOf)
        return name.uppercase().takeIf { it in HTTP_METHODS }
    }

    private fun httpMethodArgumentOf(call: UCallExpression, method: PsiMethod): String? {
        val index = method.parameterList.parameters.indexOfFirst { it.type.canonicalText == SpringWebClasses.HTTP_METHOD }
        if (index == -1) return null
        return call.getArgumentForParameter(index)?.let(::httpMethodNameOf)
    }

    private fun httpMethodNameOf(argument: UExpression): String? =
        (argument.evaluate() as? String ?: argument.asSourceString().substringAfterLast('.'))
            .uppercase().takeIf { it in HTTP_METHODS }

    private fun callOf(expression: UExpression?): UCallExpression? = when (expression) {
        is UCallExpression -> expression
        is UQualifiedReferenceExpression -> expression.selector as? UCallExpression
        else -> null
    }

    /** Every call of the one expression [call] is part of, left to right: `newBuilder()`, `uri(...)`, `GET()`, `build()`. */
    private fun chainOf(call: UCallExpression): List<UCallExpression> {
        var top: UExpression = call
        while (true) {
            val parent = top.uastParent as? UQualifiedReferenceExpression ?: break
            if (parent.receiver != top && parent.selector != top) break
            top = parent
        }
        return callsIn(top)
    }

    private fun callsIn(expression: UExpression): List<UCallExpression> = when (expression) {
        is UQualifiedReferenceExpression -> callsIn(expression.receiver) + listOfNotNull(expression.selector as? UCallExpression)
        is UCallExpression -> listOf(expression)
        else -> emptyList()
    }

    private fun restOperationsOf(owner: String): List<Receiver> =
        FIXED_VERBS.map { (method, verb) -> Receiver(owner, method, Verb.Fixed(verb)) } +
                listOf("exchange", "execute").map { Receiver(owner, it, Verb.HttpMethodArgument) }

    private const val GET = "GET"
    private const val BUILD = "build"
    private const val METHOD = "method"
    private val HTTP_METHODS = setOf("GET", "HEAD", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "TRACE")
    private val URL_TYPES = setOf("java.lang.String", "java.net.URI")
}
