/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.util

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.providers.EndpointRunLineMarkerProvider
import com.intellij.psi.JavaPsiFacade

/**
 * The prefix a servlet application declares in front of every mapping, and the servers the plugin builds requests for.
 *
 * Spring MVC serves under `server.servlet.context-path`, then under the dispatcher servlet's `spring.mvc.servlet.path`.
 * A request built from the mapping alone - "Run in Swagger", the HTTP client completion - went to a path the
 * application never serves when either is set.
 */
class ApplicationBasePathTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWebMvc_6_0_7,
    )

    fun testContextPathAndServletPathAreJoined() {
        myFixture.addFileToProject(
            "application.yml",
            "server:\n  servlet:\n    context-path: /t/\nspring:\n  mvc:\n    servlet:\n      path: /api\n"
        )

        assertEquals("/t/api", ApplicationBasePath.of(module))
    }

    fun testNothingDeclaredMeansNoBasePath() {
        myFixture.addFileToProject("application.yml", "server:\n  port: 9000\n")

        assertNull(ApplicationBasePath.of(module))
    }

    fun testRootContextPathIsNoPrefix() {
        myFixture.addFileToProject("application.properties", "server.servlet.context-path=/\n")

        assertNull(ApplicationBasePath.of(module))
    }

    fun testPlaceholderInTheContextPathIsResolved() {
        myFixture.addFileToProject(
            "application.properties",
            "server.servlet.context-path=\${app.base:/fallback}\napp.base=/shop\n"
        )

        assertEquals("/shop", ApplicationBasePath.of(module))
    }

    /** A value that only an environment variable supplies is unknown here, and a guessed prefix would be wrong. */
    fun testUnresolvableContextPathIsNotAPrefix() {
        myFixture.addFileToProject("application.properties", "server.servlet.context-path=\${CONTEXT_PATH}\n")

        assertNull(ApplicationBasePath.of(module))
    }

    /** Boot ignores the reactive key in a servlet application, so it must not become a prefix either. */
    fun testReactiveKeyIsIgnoredInAServletApplication() {
        assertNotNull(
            "The fixture must be a servlet application",
            JavaPsiFacade.getInstance(project).findClass(
                "org.springframework.web.servlet.DispatcherServlet", module.moduleWithLibrariesScope
            )
        )
        myFixture.addFileToProject("application.yml", "spring:\n  webflux:\n    base-path: /reactive\n")

        assertNull(ApplicationBasePath.of(module))
    }

    fun testRequestServersCarryTheDeclaredBasePath() {
        myFixture.addFileToProject("application.yml", "server:\n  port: 9000\n  servlet:\n    context-path: /t\n")
        val controller = myFixture.addClass("package com.example; public class Anchor {}")

        assertEquals(
            listOf("http://localhost:9000/t", "http://localhost:8080/t"),
            EndpointRunLineMarkerProvider.applyServerPortSettings(controller)
        )
    }
}
