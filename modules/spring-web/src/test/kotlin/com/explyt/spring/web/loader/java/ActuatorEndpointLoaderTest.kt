/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader

class ActuatorEndpointLoaderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    fun testPlainEndpointIsListedWithItsOperations() {
        myFixture.addFileToProject(
            "OutboxEndpoint.java",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.actuate.endpoint.annotation.Selector;
            import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;

            @Endpoint(id = "outbox")
            public class OutboxEndpoint {
                @ReadOperation
                public String status(@Selector String registration) { return registration; }

                @WriteOperation
                public String reset() { return "reset"; }
            }
            """.trimIndent()
        )

        assertEquals(
            setOf("/actuator/outbox/{registration}" to "GET", "/actuator/outbox" to "POST"),
            actuatorEndpoints().map { it.path to it.requestMethods.single() }.toSet()
        )
    }

    /**
     * Reads through the extension point rather than through a directly instantiated loader, so the test also proves
     * the loader is registered and reachable.
     */
    private fun actuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(module.project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
}
