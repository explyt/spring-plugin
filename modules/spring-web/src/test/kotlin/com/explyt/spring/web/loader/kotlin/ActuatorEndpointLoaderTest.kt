/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.intellij.psi.PsiMethod

/**
 * The reported case is Kotlin: the endpoint is an `internal class` that the annotation search sees as a light class,
 * and the management server runs on its own port.
 */
class ActuatorEndpointLoaderTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    fun testInternalKotlinWebEndpointIsListedWithItsOperations() {
        addOutboxPublishersEndpoint()

        val endpoints = actuatorEndpoints()

        assertEquals(
            listOf(
                "/actuator/outboxpublishers/{registration}" to "DELETE",
                "/actuator/outboxpublishers/{registration}" to "GET",
                "/actuator/outboxpublishers" to "POST"
            ),
            endpoints.map { it.path to it.requestMethods.single() }.sortedBy { it.second }
        )
    }

    fun testOperationsNavigateToTheirOwnMethod() {
        addOutboxPublishersEndpoint()

        val navigationTargets = actuatorEndpoints()
            .map { (it.psiElement as PsiMethod).name to it.containingClass?.name }
            .toSet()

        assertEquals(
            setOf(
                "status" to "OutboxPublishersEndpoint",
                "reset" to "OutboxPublishersEndpoint",
                "drop" to "OutboxPublishersEndpoint"
            ),
            navigationTargets
        )
    }

    fun testJmxOnlyEndpointIsNotListed() {
        myFixture.addFileToProject(
            "CacheStatsEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.jmx.annotation.JmxEndpoint

            @JmxEndpoint(id = "cachestats")
            class CacheStatsEndpoint {
                @ReadOperation
                fun stats() = "stats"
            }
            """.trimIndent()
        )

        assertEquals(emptyList<String>(), actuatorEndpoints().map { it.path })
    }

    fun testEndpointWithoutIdIsNotListed() {
        myFixture.addFileToProject(
            "NamelessEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint
            class NamelessEndpoint {
                @ReadOperation
                fun read() = "read"
            }
            """.trimIndent()
        )

        assertEquals(emptyList<String>(), actuatorEndpoints().map { it.path })
    }

    fun testCustomBasePathAndPathMappingChangeTheDisplayedPath() {
        addOutboxPublishersEndpoint()
        myFixture.addFileToProject(
            "application.properties",
            """
            management.endpoints.web.base-path=/manage
            management.endpoints.web.path-mapping.outboxpublishers=outbox
            """.trimIndent()
        )

        assertEquals(
            setOf("/manage/outbox", "/manage/outbox/{registration}"),
            actuatorEndpoints().map { it.path }.toSet()
        )
    }

    /**
     * A management port moves the endpoint to another origin, not to another path. Carrying the origin in the path
     * made the same endpoint a different element per module, so a module declaring the port listed it a second time.
     */
    fun testManagementPortDoesNotChangeTheDisplayedPath() {
        addOutboxPublishersEndpoint()
        myFixture.addFileToProject("application.properties", "management.server.port=9090")

        assertEquals(
            setOf("/actuator/outboxpublishers", "/actuator/outboxpublishers/{registration}"),
            actuatorEndpoints().map { it.path }.toSet()
        )
    }

    /**
     * The path is what a URL literal is matched against, and a host in it survives `simplifyUrl` as a path segment,
     * so a management port used to silently disable navigation from every URL string to its Actuator endpoint.
     */
    fun testUrlLiteralResolvesToTheEndpointWhenManagementPortIsSet() {
        addOutboxPublishersEndpoint()
        myFixture.addFileToProject("application.properties", "management.server.port=9090")

        val matched = SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .flatMap { it.getEndpointElements("/actuator/outboxpublishers", module) }
            .toList()

        assertEquals(listOf("/actuator/outboxpublishers"), matched.map { it.path })
    }

    private fun addOutboxPublishersEndpoint() {
        myFixture.addFileToProject(
            "OutboxPublishersEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.Access
            import org.springframework.boot.actuate.endpoint.annotation.DeleteOperation
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.annotation.Selector
            import org.springframework.boot.actuate.endpoint.annotation.WriteOperation
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint

            @WebEndpoint(id = "outboxpublishers", defaultAccess = Access.NONE)
            internal class OutboxPublishersEndpoint {
                @ReadOperation
                fun status(@Selector registration: String) = registration

                @WriteOperation
                fun reset() = "reset"

                @DeleteOperation
                fun drop(@Selector registration: String) = registration
            }
            """.trimIndent()
        )
    }

    /**
     * Reads through the extension point rather than through a directly instantiated loader, so the test also proves
     * the loader is registered and reachable.
     */
    private fun actuatorEndpoints(): List<EndpointElement> {
        return SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
    }
}
