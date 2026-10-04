/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view

import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.util.ExplytPsiUtil.toSmartPointer

/** The rows of the Endpoints tool window and the tree it groups them into: by endpoint type, then by class or file. */
object EndpointsTreeData {

    /** One row per HTTP method of [element]; none for an endpoint with neither a class nor a file to show it under. */
    fun rowsOf(element: EndpointElement): List<EndpointElementViewData> {
        val classOrFileName = element.containingClass?.name
            ?: element.containingFile?.name ?: return emptyList()
        return rowVerbsOf(element).asSequence()
            .map {
                EndpointElementViewData(
                    element.type, element.psiElement.toSmartPointer(), classOrFileName, it, element.path,
                    element.exposure, element.access
                )
            }
            .sortedBy { it.classOrFileName + it.method }
            .toList()
    }

    /**
     * The verbs [element] is listed under, one row each. An endpoint that names no verb - a bare `@RequestMapping`, or
     * an Actuator endpoint that declares no operation - gets a single unnamed row rather than none, so it stays listed.
     */
    fun rowVerbsOf(element: EndpointElement): List<String> = element.requestMethods.ifEmpty { listOf(NO_VERB) }

    /** [rows] grouped by endpoint type, in the order of [EndpointType], then by class or file name. */
    fun byType(rows: List<EndpointElementViewData>): List<EndpointViewByType> {
        val rowsByType = rows.groupBy { it.type }
        return EndpointType.entries.mapNotNull { type ->
            rowsByType[type]?.let { EndpointViewByType(type, byContainer(it)) }
        }
    }

    private fun byContainer(rows: List<EndpointElementViewData>): List<EndpointViewWithContainerName> =
        rows.groupBy { it.classOrFileName }.asSequence()
            .map { (name, list) -> EndpointViewWithContainerName(name, list.sortedBy { it.method }) }
            .sortedBy { it.classOrFileName }
            .toList()

    private const val NO_VERB = ""
}
