/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNotNull
import junit.framework.TestCase.assertTrue

/**
 * Every page of an `explyt_get_spring_http_endpoints` answer, read the way a client continues it: from `nextOffset`
 * with the first page's revision until `nextOffset` is null. A test about the whole listing must not depend on the
 * listing fitting the default budget.
 */
suspend fun SpringBootApplicationMcpToolset.endpointListingPages(
    projectPath: String?,
    endpointType: String = "",
    controllerFilter: String = "",
    compact: Boolean = false,
    maxChars: Int? = null,
    mapper: ObjectMapper = ObjectMapper(),
): List<JsonNode> {
    val pages = mutableListOf<JsonNode>()
    var offset = 0
    var revision: String? = null
    while (true) {
        val page = mapper.readTree(
            getHttpEndpoints(
                projectPath = projectPath,
                controllerFilter = controllerFilter,
                endpointType = endpointType,
                offset = offset,
                compact = compact,
                maxChars = maxChars,
                expectedRevision = revision,
            )
        )
        assertEquals("A listing page is served, got $page", "OK", page["status"]?.asText())
        pages.add(page)
        val next = page["nextOffset"]
        assertNotNull("A listing page carries nextOffset, null on the last one, got $page", next)
        if (next.isNull) return pages
        assertTrue("A continuation must advance past $offset, got $next", next.asInt() > offset)
        assertTrue("The listing did not terminate", pages.size <= MAX_LISTING_PAGES)
        offset = next.asInt()
        revision = page["revision"].asText()
    }
}

suspend fun SpringBootApplicationMcpToolset.allListedEndpoints(
    projectPath: String?,
    endpointType: String = "",
    controllerFilter: String = "",
    compact: Boolean = false,
    mapper: ObjectMapper = ObjectMapper(),
): List<JsonNode> =
    endpointListingPages(projectPath, endpointType, controllerFilter, compact, mapper = mapper)
        .flatMap { it["endpoints"].toList() }

private const val MAX_LISTING_PAGES = 100
