/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.kotlin

import com.explyt.spring.core.completion.properties.ActuatorEndpointConfigurationPropertiesLoader
import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.properties.references.ActuatorEndpointValueTypeReference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiReference

class ActuatorEndpointConfigurationPropertiesLoaderPre34Test : ExplytKotlinLightTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java)
    }

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

    fun testAccessIsUnresolvedBeforeSpringBoot34() {
        addEndpoint()
        myFixture.configureByText(
            "application.properties",
            "management.endpoint.legacy.access=unrestricted"
        )

        val unresolved = myFixture.doHighlighting()
            .mapNotNull { it.description }
            .filter { it.contains("Cannot resolve key property") }

        assertEquals(1, unresolved.size)
    }

    fun testAccessHasNoActuatorReferenceBeforeSpringBoot34() {
        addEndpoint()
        myFixture.configureByText(
            "application.properties",
            """
            management.endpoint.legacy.access=unrestricted
            management.endpoint.legacy.enabled=true
            management.endpoint.legacy.cache.time-to-live=10s
            """.trimIndent()
        )

        val accessReference = referenceAt("access")
        assertFalse(accessReference is ActuatorEndpointValueTypeReference)
        assertTrue(referenceAt("enabled") is ActuatorEndpointValueTypeReference)
        assertTrue(referenceAt("time-to-live") is ActuatorEndpointValueTypeReference)
    }

    private fun referenceAt(text: String): PsiReference? {
        val offset = myFixture.file.text.lastIndexOf(text) + text.length - 1
        return myFixture.file.findReferenceAt(offset)
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
