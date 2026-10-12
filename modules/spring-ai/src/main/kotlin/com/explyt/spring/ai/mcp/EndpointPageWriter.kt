/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.entities.EntityInventory
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.web.loader.EndpointElement
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project

class EndpointPageWriter(private val project: Project) {

    fun write(
        envelope: ObjectNode,
        endpoints: List<EndpointElement>,
        toJson: (EndpointElement) -> Any,
        query: Map<String, String?>,
        page: PageRequest,
        nearestByPrefix: List<Any>? = null,
    ): String {
        val beanFilterEnabled = SpringToolRunConfigurationsSettingsState.getInstance().isBeanFilterEnabled
        val revision = EntityInventory.revision(project, query + ("beanFilterEnabled" to beanFilterEnabled.toString()))
        val withNeighbourhood = when {
            nearestByPrefix == null -> envelope
            page.offset == 0 -> writer.fitSample(
                envelope, FIELD_ENDPOINTS, FIELD_NEAREST, nearestByPrefix.map(::toNode),
                FIELD_NEAREST_TRUNCATED, endpoints.size, revision, page
            )

            else -> envelope.deepCopy().apply { putArray(FIELD_NEAREST) }
        }
        return writer.write(
            envelope = withNeighbourhood,
            itemsField = FIELD_ENDPOINTS,
            totalCount = endpoints.size,
            itemAt = { index ->
                ProgressManager.checkCanceled()
                toNode(toJson(endpoints[index]))
            },
            revision = revision,
            page = page,
        )
    }

    private fun toNode(value: Any): ObjectNode = mapper.valueToTree(value)

    private val writer = BoundedPageWriter()

    private companion object {
        val mapper = ObjectMapper()
        const val FIELD_ENDPOINTS = "endpoints"
        const val FIELD_NEAREST = "nearestByPrefix"
        const val FIELD_NEAREST_TRUNCATED = "nearestByPrefixTruncated"
    }
}
