/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.route

sealed interface RouteVerbs {

    infix fun and(other: RouteVerbs): RouteVerbs

    fun servedVerbsOf(routeVerbs: List<String>): List<String>?

    data object Unrestricted : RouteVerbs {
        override fun and(other: RouteVerbs): RouteVerbs = other

        override fun servedVerbsOf(routeVerbs: List<String>): List<String> = routeVerbs
    }

    data object Unknown : RouteVerbs {
        override fun and(other: RouteVerbs): RouteVerbs = other as? Only ?: this

        override fun servedVerbsOf(routeVerbs: List<String>): List<String> = routeVerbs
    }

    data class Only(val verbs: Set<String>) : RouteVerbs {
        override fun and(other: RouteVerbs): RouteVerbs = when (other) {
            is Only -> Only(verbs intersect other.verbs)
            Unrestricted, Unknown -> this
        }

        override fun servedVerbsOf(routeVerbs: List<String>): List<String>? =
            routeVerbs.ifEmpty { verbs.toList() }.filter { it in verbs }.ifEmpty { null }
    }

    companion object {
        fun of(verbs: List<String?>): RouteVerbs =
            verbs.takeIf { it.isNotEmpty() && null !in it }?.let { Only(it.filterNotNull().toSet()) } ?: Unknown
    }
}
