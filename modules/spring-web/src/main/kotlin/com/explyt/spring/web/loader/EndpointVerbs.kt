/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

object EndpointVerbs {

    const val ALL = "ALL"

    val OPENAPI_METHODS = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS", "HEAD")

    fun openApiMethodsOf(requestMethods: List<String>, siblingVerbs: Collection<String> = emptyList()): List<String> =
        requestMethods.ifEmpty {
            OPENAPI_METHODS.filter { verb -> siblingVerbs.none { it.equals(verb, ignoreCase = true) } }
        }

    fun labelsOf(endpoint: EndpointElement): List<String> = endpoint.requestMethods.ifEmpty { listOf(ALL) }

    fun answers(label: String, verb: String): Boolean = label == ALL || label.equals(verb, ignoreCase = true)

    fun answers(endpoint: EndpointElement, verb: String): Boolean = answers(endpoint.requestMethods, verb)

    fun answers(requestMethods: List<String>, verb: String?): Boolean =
        requestMethods.isEmpty() || verb != null && requestMethods.any { it.equals(verb, ignoreCase = true) }
}
