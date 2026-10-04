/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.loader.EndpointAccess
import com.explyt.spring.web.loader.EndpointAccess.NONE
import com.explyt.spring.web.loader.EndpointAccess.READ_ONLY
import com.explyt.spring.web.loader.EndpointAccess.UNKNOWN
import com.explyt.spring.web.loader.EndpointAccess.UNRESTRICTED
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.view.EndpointElementViewData
import com.explyt.spring.web.view.nodes.HttpMethodNode
import com.explyt.spring.web.view.nodes.RootEndpointNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope

/**
 * The access Boot grants each Actuator operation is a fact separate from its exposure: an exposed endpoint without
 * access answers 404. Nothing is hidden for it.
 */
class ActuatorAccessTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0, TestLibrary.springBootHealth_4_1_0)

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        assertNotNull(
            "precondition: Boot's ShutdownEndpoint is on the classpath",
            JavaPsiFacade.getInstance(project).findClass("org.springframework.boot.actuate.context.ShutdownEndpoint", scope)
        )
        assertNotNull(
            "precondition: Boot's LoggersEndpoint, which has a write operation, is on the classpath",
            JavaPsiFacade.getInstance(project).findClass("org.springframework.boot.actuate.logging.LoggersEndpoint", scope)
        )
    }

    fun testDefaultConfigurationGrantsNoneToShutdownAndHeapDumpOnly() {
        assertEquals(NONE, accessOf("/actuator/shutdown", "POST"))
        assertEquals(NONE, accessOf("/actuator/heapdump", "GET"))
        assertEquals(UNRESTRICTED, accessOf("/actuator/health", "GET"))
        assertEquals(UNRESTRICTED, accessOf("/actuator/loggers/{name}", "POST"))
    }

    fun testPerEndpointAccessOverridesTheDeclaredDefault() {
        addProperties("management.endpoint.shutdown.access=unrestricted")

        assertEquals(UNRESTRICTED, accessOf("/actuator/shutdown", "POST"))
        assertEquals(NONE, accessOf("/actuator/heapdump", "GET"))
    }

    fun testReadOnlyDefaultLeavesReadOperationsAndDeniesWrites() {
        addProperties("management.endpoints.access.default=read-only")

        assertEquals(READ_ONLY, accessOf("/actuator/loggers/{name}", "GET"))
        assertEquals(NONE, accessOf("/actuator/loggers/{name}", "POST"))
        assertEquals(READ_ONLY, accessOf("/actuator/health", "GET"))
    }

    fun testMaxPermittedCapsAnUnrestrictedOverride() {
        addProperties(
            """
            management.endpoint.loggers.access=unrestricted
            management.endpoints.access.max-permitted=read-only
            """.trimIndent()
        )

        assertEquals(READ_ONLY, accessOf("/actuator/loggers/{name}", "GET"))
        assertEquals(NONE, accessOf("/actuator/loggers/{name}", "POST"))
    }

    fun testLegacyEnabledKeyReadsAsAccess() {
        addProperties(
            """
            management.endpoint.shutdown.enabled=true
            management.endpoint.health.enabled=false
            """.trimIndent()
        )

        assertEquals(UNRESTRICTED, accessOf("/actuator/shutdown", "POST"))
        assertEquals(NONE, accessOf("/actuator/health", "GET"))
    }

    fun testAccessKeySetTogetherWithItsLegacyTwinIsUnknown() {
        addProperties(
            """
            management.endpoint.shutdown.access=unrestricted
            management.endpoint.shutdown.enabled=true
            """.trimIndent()
        )

        assertEquals("Boot refuses to start with both keys", UNKNOWN, accessOf("/actuator/shutdown", "POST"))
    }

    fun testUnresolvablePlaceholderLeavesTheAccessUnknown() {
        addProperties("management.endpoint.shutdown.access=\${shutdown.access}")

        assertEquals(UNKNOWN, accessOf("/actuator/shutdown", "POST"))
        assertEquals(UNRESTRICTED, accessOf("/actuator/health", "GET"))
    }

    fun testResolvedPlaceholderIsRead() {
        addProperties(
            """
            shutdown.access=unrestricted
            management.endpoint.shutdown.access=${'$'}{shutdown.access}
            """.trimIndent()
        )

        assertEquals(UNRESTRICTED, accessOf("/actuator/shutdown", "POST"))
    }

    fun testAnEndpointWithoutAccessIsStillListedAndExposureIsIndependent() {
        addProperties("management.endpoints.web.exposure.include=*")

        val shutdown = elementsAt("/actuator/shutdown").single()
        assertEquals(EndpointExposure.EXPOSED, shutdown.exposure)
        assertEquals(NONE, shutdown.access)
    }

    fun testToolWindowShowsAnEndpointWithoutAccessAsSuch() {
        val health = JavaPsiFacade.getInstance(project).findClass(
            "org.springframework.boot.health.actuate.endpoint.HealthEndpoint", GlobalSearchScope.allScope(project)
        )!!
        fun renderedText(access: EndpointAccess?): String {
            val viewData = EndpointElementViewData(
                EndpointType.ACTUATOR, SmartPointerManager.createPointer<PsiElement>(health),
                "HealthEndpoint", "GET", "/actuator/health", EndpointExposure.EXPOSED, access
            )
            val node = HttpMethodNode(viewData, RootEndpointNode(emptyList()))
            node.update()
            return node.presentation.coloredText.joinToString("") { it.text }
        }

        val none = SpringWebBundle.message("explyt.web.endpoints.tool.actuator.access.none")
        assertEquals("/actuator/health  $none", renderedText(NONE))
        assertFalse("an endpoint with access carries no note", renderedText(UNRESTRICTED).contains(none))
        assertFalse("read-only still serves its read operation", renderedText(READ_ONLY).contains(none))
    }

    private fun addProperties(text: String) {
        myFixture.addFileToProject("application.properties", text)
    }

    private fun accessOf(path: String, method: String): EndpointAccess {
        val matching = elementsAt(path).filter { method in it.requestMethods }
        assertFalse(
            "expected $method $path among ${actuatorEndpoints().map { "${it.requestMethods} ${it.path}" }.sorted()}",
            matching.isEmpty()
        )
        return matching.map { it.access }.distinct().single() ?: error("$method $path carries no access")
    }

    private fun elementsAt(path: String): List<EndpointElement> = actuatorEndpoints().filter { it.path == path }

    private fun actuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
}
