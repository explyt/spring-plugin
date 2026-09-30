/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view

import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.util.ApplicationBasePath
import com.explyt.spring.web.util.EndpointUrlMatcher
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleUtilCore

/**
 * The rows the Endpoints tool window shows for a search text and the selected HTTP methods and endpoint types.
 *
 * A text naming a path - `/api/items/42`, or a URL pasted from a browser, a log line or a curl - is matched the way the
 * application dispatches it: against the `{templates}` of the routes, under the base path the application declares,
 * and failing that under a leading prefix only the deployment knows. Any other text is a fragment of a route, found
 * wherever it occurs.
 *
 * The URL is matched against every row before the methods and types narrow them: a URL that exists only for another
 * verb has been found, and reading it under one more dropped segment to satisfy the filter would show a different URL.
 */
object EndpointsViewFilter {

    fun apply(
        rows: List<EndpointElementViewData>,
        searchText: String,
        httpMethods: Set<String>,
        endpointTypes: Set<EndpointType>,
        basePathOf: (EndpointElementViewData) -> String?,
    ): List<EndpointElementViewData> {
        val text = searchText.trim()
        val found = when {
            text.length < MIN_SEARCH_LENGTH -> rows
            text.isPath() -> EndpointUrlMatcher.match(rows, text, EndpointUrlMatcher.Policy.SEARCH, { it.path }, basePathOf).endpoints
            else -> rows.filter { it.path.contains(text, ignoreCase = true) }
        }
        return found.filter { (endpointTypes.isEmpty() || it.type in endpointTypes) && (httpMethods.isEmpty() || it.method in httpMethods) }
    }

    /** The base path the application of each row declares, read once per module for the lifetime of the function. */
    fun declaredBasePaths(): (EndpointElementViewData) -> String? {
        val byModule = HashMap<Module, String?>()
        return { row ->
            row.psiPointer.element
                ?.let(ModuleUtilCore::findModuleForPsiElement)
                ?.let { module -> byModule.getOrPut(module) { ApplicationBasePath.cachedOf(module) } }
        }
    }

    private fun String.isPath(): Boolean = contains('/')

    private const val MIN_SEARCH_LENGTH = 2
}
