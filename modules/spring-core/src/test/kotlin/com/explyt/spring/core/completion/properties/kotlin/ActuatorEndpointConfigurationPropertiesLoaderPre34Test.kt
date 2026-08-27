/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.kotlin

import com.explyt.spring.core.completion.properties.ActuatorEndpointConfigurationPropertiesLoader
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary

class ActuatorEndpointConfigurationPropertiesLoaderPre34Test : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework.boot:spring-boot-actuator-autoconfigure:3.3.0")
    )

    fun testAccessIsNotSynthesizedBeforeSpringBoot34() {
        addEndpoint()

        val names = ActuatorEndpointConfigurationPropertiesLoader()
            .loadProperties(myFixture.module)
            .map { it.name }
            .toSet()

        assertEquals(
            setOf(
                "management.endpoint.legacy.enabled",
                "management.endpoint.legacy.cache.time-to-live"
            ),
            names
        )
    }

    private fun addEndpoint() {
        myFixture.addFileToProject(
            "LegacyEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint

            @WebEndpoint(id = "legacy")
            internal class LegacyEndpoint
            """.trimIndent()
        )
    }
}
