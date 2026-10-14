/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.entities.EntityInventory
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.beans.BeanConditionRecord
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
        val withLimitations = envelope.withConditionLimitation(beanFilterEnabled)
        val withNeighbourhood = when {
            nearestByPrefix == null -> withLimitations
            page.offset == 0 -> writer.fitSample(
                withLimitations, FIELD_ENDPOINTS, FIELD_NEAREST, nearestByPrefix.map(::toNode),
                FIELD_NEAREST_TRUNCATED, endpoints.size, revision, page
            )

            else -> withLimitations.deepCopy().apply { putArray(FIELD_NEAREST) }
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

    private fun ObjectNode.withConditionLimitation(beanFilterEnabled: Boolean): ObjectNode =
        if (beanFilterEnabled) this
        else deepCopy().apply { putArray(FIELD_LIMITATIONS).add(BeanConditionRecord.CONDITIONS_NOT_EVALUATED) }

    private fun toNode(value: Any): ObjectNode = mapper.valueToTree(value)

    private val writer = BoundedPageWriter()

    private companion object {
        val mapper = ObjectMapper()
        const val FIELD_ENDPOINTS = "endpoints"
        const val FIELD_NEAREST = "nearestByPrefix"
        const val FIELD_NEAREST_TRUNCATED = "nearestByPrefixTruncated"
        const val FIELD_LIMITATIONS = "limitations"
    }
}
