/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader

object EndpointVerbs {

    const val ALL = "ALL"

    fun labelsOf(endpoint: EndpointElement): List<String> = endpoint.requestMethods.ifEmpty { listOf(ALL) }

    fun answers(label: String, verb: String): Boolean = label == ALL || label == verb
}
