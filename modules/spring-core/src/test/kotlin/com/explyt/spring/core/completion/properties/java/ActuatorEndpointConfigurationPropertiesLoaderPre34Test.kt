/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion.properties.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.completion.properties.ActuatorEndpointConfigurationPropertiesLoader
import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.properties.references.ActuatorEndpointValueTypeReference
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiReference
import com.intellij.psi.search.GlobalSearchScope

class ActuatorEndpointConfigurationPropertiesLoaderPre34Test : ExplytJavaLightTestCase() {

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

    /**
     * The capability gate must follow the classpath the module has now, not the one it had when the endpoint
     * cache was first populated - a cached provider is reused for the lifetime of the module, so a gate
     * evaluated outside it stays frozen at whatever the first caller saw.
     */
    fun testAccessIsSynthesizedOnceTheAccessClassAppears() {
        addEndpoint()
        assertFalse("the fixture must start without the Access class", accessClassExists())
        assertFalse("access must be absent before the class appears", synthesizedNames().contains(ACCESS_KEY))

        addAccessClass()

        assertTrue("the added Access class must be resolvable", accessClassExists())
        assertTrue(
            "access must be synthesized once Access is on the classpath, got: ${synthesizedNames()}",
            synthesizedNames().contains(ACCESS_KEY)
        )
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
        assertFalse("reference=${accessReference?.javaClass?.name}", accessReference is ActuatorEndpointValueTypeReference)
        assertTrue(referenceAt("enabled") is ActuatorEndpointValueTypeReference)
        assertTrue(referenceAt("time-to-live") is ActuatorEndpointValueTypeReference)
    }

    private fun referenceAt(text: String): PsiReference? {
        val offset = myFixture.file.text.lastIndexOf(text) + text.length - 1
        return myFixture.file.findReferenceAt(offset)
    }

    private fun synthesizedNames(): Set<String> = ActuatorEndpointConfigurationPropertiesLoader()
        .loadProperties(myFixture.module)
        .mapTo(mutableSetOf()) { it.name }

    private fun accessClassExists() = JavaPsiFacade.getInstance(project)
        .findClass(SpringCoreClasses.ACTUATOR_ENDPOINT_ACCESS, GlobalSearchScope.allScope(project)) != null

    private fun addAccessClass() {
        myFixture.addClass(
            """
            package org.springframework.boot.actuate.endpoint;

            public enum Access { NONE, READ_ONLY, UNRESTRICTED }
            """.trimIndent()
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun addEndpoint() {
        myFixture.addClass(
            """
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;

            @WebEndpoint(id = "legacy")
            public class LegacyEndpoint {}
            """.trimIndent()
        )
    }

    private companion object {
        const val ACCESS_KEY = "management.endpoint.legacy.access"
    }
}
