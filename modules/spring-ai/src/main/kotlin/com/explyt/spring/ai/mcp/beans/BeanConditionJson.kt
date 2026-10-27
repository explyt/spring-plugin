/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanConditionRecord
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode

object BeanConditionJson {

    private val mapper = ObjectMapper()

    fun of(condition: BeanConditionRecord, includeAssumptions: Boolean = false): ObjectNode =
        mapper.createObjectNode().apply {
            put("state", condition.state.name)
            if (condition.undecided) putArray("reasons").apply { condition.reasons.forEach(::add) }
            put("annotation", condition.annotation)
            put("carrier", condition.carrier)
            condition.detail?.let { put("detail", it) }
            if (includeAssumptions && condition.assumptions.isNotEmpty()) {
                putArray("assumptions").apply { condition.assumptions.forEach(::add) }
            }
        }
}
