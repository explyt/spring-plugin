/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointExposure.EXPOSED
import com.explyt.spring.web.loader.EndpointExposure.NOT_EXPOSED
import com.explyt.spring.web.loader.EndpointExposure.UNKNOWN
import com.explyt.spring.web.util.ActuatorExposure.Patterns
import com.explyt.spring.web.util.ActuatorExposure.Rule
import org.junit.Assert.assertEquals
import org.junit.Test

/** The `management.endpoints.web.exposure` rule, as Spring Boot's `IncludeExcludeEndpointFilter` applies it. */
class ActuatorExposureRuleTest {

    private fun rule(include: List<String> = emptyList(), exclude: List<String> = emptyList()) =
        Rule(Patterns(include), Patterns(exclude))

    private fun Rule.exposures(vararg ids: String): Map<String, EndpointExposure> = ids.associateWith { exposureOf(it) }

    @Test
    fun `an empty include exposes Boot's default set, health alone`() {
        assertEquals(mapOf("health" to EXPOSED, "info" to NOT_EXPOSED, "env" to NOT_EXPOSED), rule().exposures("health", "info", "env"))
    }

    @Test
    fun `a listed include replaces the default set`() {
        assertEquals(
            mapOf("info" to EXPOSED, "metrics" to EXPOSED, "health" to NOT_EXPOSED),
            rule(include = listOf("info", "metrics")).exposures("info", "metrics", "health")
        )
    }

    @Test
    fun `a star includes every id`() {
        assertEquals(mapOf("env" to EXPOSED, "heapdump" to EXPOSED), rule(include = listOf("*")).exposures("env", "heapdump"))
    }

    @Test
    fun `an exclusion wins over every inclusion, the star included`() {
        assertEquals(
            mapOf("env" to NOT_EXPOSED, "info" to EXPOSED),
            rule(include = listOf("*"), exclude = listOf("env")).exposures("env", "info")
        )
        assertEquals(NOT_EXPOSED, rule(include = listOf("health"), exclude = listOf("*")).exposureOf("health"))
    }

    @Test
    fun `ids compare in EndpointId form, ignoring case, dashes and dots`() {
        val rule = rule(include = listOf("heap-dump", "Thread.Dump", " info "))

        assertEquals(mapOf("heapdump" to EXPOSED, "threaddump" to EXPOSED, "info" to EXPOSED), rule.exposures("heapdump", "threaddump", "info"))
        assertEquals("heapdump", ActuatorExposure.normalize("Heap-Dump"))
    }

    @Test
    fun `an unreadable item leaves only the ids it could name undecided`() {
        val rule = rule(include = listOf("health", "\${extra.endpoints}"))

        assertEquals(mapOf("health" to EXPOSED, "env" to UNKNOWN), rule.exposures("health", "env"))
        assertEquals(
            "an unreadable exclusion could exclude anything that is otherwise included",
            mapOf("health" to UNKNOWN, "env" to NOT_EXPOSED),
            rule(exclude = listOf("\${hidden.endpoints}")).exposures("health", "env")
        )
    }
}
