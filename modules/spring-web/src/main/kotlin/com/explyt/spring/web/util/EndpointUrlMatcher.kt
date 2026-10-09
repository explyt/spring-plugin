/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.web.util.EndpointPathPatterns.PathReading

/**
 * Which endpoints a URL addresses, when the URL is written the way a client writes it - with a scheme and a host, a
 * query, a fragment, and a context path in front of the mapping - rather than the way a mapping declares it.
 *
 * The URL is read as written first, then under a base path the configuration of an endpoint's module declares
 * ([ApplicationBasePath]), then - only when the [Policy] allows guessing - under ever longer leading prefixes no
 * configuration declares. The first reading any endpoint answers is the answer; which one it was is reported, so a
 * caller can tell a declared fact from a guess.
 *
 * Endpoints are generic: the endpoint model, the rows of a tool window and a bare route string all fit, given how to
 * read the route and the declared base path of each.
 */
object EndpointUrlMatcher {

    /**
     * How much a caller may read into a URL.
     *
     * @property anyHost whether a URL naming a host that is not this machine still addresses the project's endpoints.
     * @property guessPrefixes whether leading segments no configuration declares may be dropped.
     * @property fragments whether a text merely contained in a route, as typed, finds it.
     */
    enum class Policy(val anyHost: Boolean, val guessPrefixes: Boolean, val fragments: Boolean) {
        /**
         * A URL written in code - a MockMvc or WebClient call, a redirect. Resolving it is a claim about the code, so
         * it must be certain: a call to another host is a call to another service, and a prefix nobody declared is
         * no evidence of which endpoint is meant.
         */
        REFERENCE(anyHost = false, guessPrefixes = false, fragments = false),

        /**
         * A URL handed to a client that dispatches inside the JVM - a MockMvc request, a `WebTestClient` bound to a
         * controller or an application context. Such a request reaches the application under test whatever host it
         * names: the host only becomes the request's server name and `Host` header, and a test often sets it on
         * purpose, to exercise a tenant or a brand resolved from it. Everything else stays as certain as
         * [REFERENCE].
         */
        IN_PROCESS(anyHost = true, guessPrefixes = false, fragments = false),

        /** A URL a person pasted from a browser, a log line or a curl, or a path being typed into a search field. */
        SEARCH(anyHost = true, guessPrefixes = true, fragments = true),
    }

    enum class ReadingKind { AS_WRITTEN, DECLARED_BASE_PATH, ASSUMED_PREFIX }

    /**
     * How the request path met the routes: [path] is what was matched, after dropping [prefix] - declared or guessed,
     * as [kind] says - or `null` for a path read as written.
     */
    data class Reading(val kind: ReadingKind, val prefix: String?, val path: String)

    /** The endpoints a URL addresses, closest match first, and how the URL was read; `null` when nothing matched. */
    data class Match<E>(val endpoints: List<E>, val reading: Reading?)

    fun <E> match(
        endpoints: Collection<E>,
        url: String,
        policy: Policy,
        routeOf: (E) -> String,
        basePathOf: (E) -> String?,
    ): Match<E> {
        val requestPath = requestPathOf(url, policy) ?: return Match(emptyList(), null)
        val routes = endpoints.map { Route(it, routeOf(it)) }
        val fragment = url.trim().takeIf { policy.fragments && it.isNotEmpty() }
        return EndpointPathPatterns.preferredReading(
            readingsOf(requestPath, routes, policy, basePathOf),
            { reading -> matchesOf(routes, reading, fragment, basePathOf) },
            { SpringWebUtil.simplifyUrl(routeOf(it)) },
        )
            ?.let { (reading, matches) -> Match(matches, reading) }
            ?: Match(emptyList(), null)
    }

    /** Whether a URL written in code addresses the endpoint at [route], served under [basePath]. */
    fun addresses(route: String, url: String, basePath: String?, policy: Policy = Policy.REFERENCE): Boolean =
        match(listOf(route), url, policy, { it }, { basePath }).endpoints.isNotEmpty()

    /**
     * The request path of [url], or `null` when the [policy] does not let a URL naming another host address the
     * project: `http://localhost:8080/api/items` is `/api/items` either way, `https://other-host/api/items` only
     * for a search.
     */
    fun requestPathOf(url: String, policy: Policy): String? {
        val text = url.trim()
        val authority = ORIGIN.find(text)?.groupValues?.get(1)
        if (authority != null && !policy.anyHost && !isThisMachine(authority)) return null
        return EndpointPathPatterns.requestPathOf(text)
    }

    private fun <E> readingsOf(
        requestPath: String,
        routes: List<Route<E>>,
        policy: Policy,
        basePathOf: (E) -> String?,
    ): Sequence<Reading> = sequence {
        yield(Reading(ReadingKind.AS_WRITTEN, prefix = null, path = requestPath))
        routes.asSequence()
            .mapNotNull { basePathOf(it.endpoint) }
            .distinct()
            .filter { requestPath == it || requestPath.startsWith("$it/") }
            .sortedByDescending { it.length }
            .forEach { yield(Reading(ReadingKind.DECLARED_BASE_PATH, it, requestPath.removePrefix(it).ifEmpty { "/" })) }
        if (policy.guessPrefixes) {
            EndpointPathPatterns.readingsOf(requestPath).drop(1)
                .forEach { yield(Reading(ReadingKind.ASSUMED_PREFIX, it.assumedPrefix, it.path)) }
        }
    }

    private fun <E> matchesOf(
        routes: List<Route<E>>,
        reading: Reading,
        fragment: String?,
        basePathOf: (E) -> String?,
    ): List<E> =
        routes.asSequence()
            .filter { admits(reading, it, basePathOf) }
            .mapNotNull { route -> rankOf(route, reading, fragment)?.let { Ranked(route, it) } }
            .sortedWith(compareBy<Ranked<E>> { it.rank }.thenBy(EndpointPathPatterns.SPECIFICITY) { it.route.path })
            .map { it.route.endpoint }
            .toList()

    /**
     * Under a declared base path only the routes of a module declaring that very base path answer - another
     * application does not serve under it. A guessed prefix keeps the rule of [PathReading.admits].
     */
    private fun <E> admits(reading: Reading, route: Route<E>, basePathOf: (E) -> String?): Boolean =
        when (reading.kind) {
            ReadingKind.AS_WRITTEN -> true
            ReadingKind.DECLARED_BASE_PATH -> basePathOf(route.endpoint) == reading.prefix
            ReadingKind.ASSUMED_PREFIX -> PathReading(reading.prefix, reading.path).admits(route.path)
        }

    /**
     * How closely [route] answers [reading], lowest first. A fragment finds a route only in the path as written: under
     * a dropped prefix the fragment would be one the matcher cut out itself.
     */
    private fun rankOf(route: Route<*>, reading: Reading, fragment: String?): Int? = when {
        route.path == reading.path -> EXACT
        SpringWebUtil.isEndpointMatches(route.path, reading.path) -> PATTERN
        reading.kind == ReadingKind.AS_WRITTEN && fragment != null
                && route.declared.contains(fragment, ignoreCase = true) -> FRAGMENT
        else -> null
    }

    private fun isThisMachine(authority: String): Boolean {
        val hostAndPort = authority.substringAfterLast('@')
        val host = if (hostAndPort.startsWith('[')) hostAndPort.substringBefore(']') + "]" else hostAndPort.substringBefore(':')
        return host.isEmpty() || host.lowercase() in THIS_MACHINE
    }

    private class Route<E>(val endpoint: E, val declared: String) {
        val path: String = SpringWebUtil.simplifyUrl(declared)
    }

    private class Ranked<E>(val route: Route<E>, val rank: Int)

    private val ORIGIN = Regex("^[A-Za-z][A-Za-z0-9+.-]*://([^/?#]*)")
    private val THIS_MACHINE = setOf("localhost", "127.0.0.1", "0.0.0.0", "[::1]")
    private const val EXACT = 0
    private const val PATTERN = 1
    private const val FRAGMENT = 2
}
