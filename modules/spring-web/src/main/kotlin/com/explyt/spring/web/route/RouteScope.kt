/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.route

data class RouteScope(val prefix: RoutePrefix, val verbs: RouteVerbs) {

    val isUndecidable: Boolean
        get() = prefix == RoutePrefix.Undecidable

    val paths: List<String>
        get() = (prefix as? RoutePrefix.Decided)?.segments.orEmpty()

    infix fun then(inner: RouteScope): RouteScope = RouteScope(prefix.then(inner.prefix), verbs and inner.verbs)

    companion object {
        val ROOT = RouteScope(RoutePrefix.PathFree, RouteVerbs.Unrestricted)
        val UNDECIDABLE = RouteScope(RoutePrefix.Undecidable, RouteVerbs.Unknown)

        fun of(prefix: RoutePrefix): RouteScope = RouteScope(prefix, RouteVerbs.Unrestricted)
    }
}
