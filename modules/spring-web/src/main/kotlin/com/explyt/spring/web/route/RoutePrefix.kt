/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.route

sealed interface RoutePrefix {

    fun then(inner: RoutePrefix): RoutePrefix

    data class Decided(val segments: List<String>) : RoutePrefix {
        override fun then(inner: RoutePrefix): RoutePrefix = when (inner) {
            is Decided -> Decided(segments.flatMap { outer -> inner.segments.map { outer + it } })
            PathFree -> this
            Undecidable -> Undecidable
        }
    }

    data object PathFree : RoutePrefix {
        override fun then(inner: RoutePrefix): RoutePrefix = inner
    }

    data object Undecidable : RoutePrefix {
        override fun then(inner: RoutePrefix): RoutePrefix = this
    }

    companion object {
        fun of(segments: List<String>): RoutePrefix = if (segments.isEmpty()) Undecidable else Decided(segments)
    }
}
