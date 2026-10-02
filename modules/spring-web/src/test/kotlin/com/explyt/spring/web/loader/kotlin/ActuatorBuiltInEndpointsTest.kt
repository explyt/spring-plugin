/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointExposure
import com.explyt.spring.web.loader.EndpointExposure.EXPOSED
import com.explyt.spring.web.loader.EndpointExposure.NOT_EXPOSED
import com.explyt.spring.web.loader.EndpointExposure.UNKNOWN
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.explyt.spring.web.SpringWebBundle
import com.explyt.spring.web.view.EndpointElementViewData
import com.explyt.spring.web.view.nodes.HttpMethodNode
import com.explyt.spring.web.view.nodes.RootEndpointNode
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.GlobalSearchScope

/**
 * Boot's own Actuator endpoints - `health`, `info`, `env`... - come from the libraries, not from the project, and the
 * configuration decides which of them answer over HTTP. They are listed whether exposed or not: the configuration a
 * deployment runs with may differ from the files in the project.
 */
class ActuatorBuiltInEndpointsTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0, TestLibrary.springBootHealth_4_1_0)

    override fun setUp() {
        super.setUp()
        val scope = GlobalSearchScope.allScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: Boot's HealthEndpoint is on the classpath",
            facade.findClass("org.springframework.boot.health.actuate.endpoint.HealthEndpoint", scope)
        )
        assertNotNull(
            "precondition: InfoEndpoint is on the classpath",
            facade.findClass("org.springframework.boot.actuate.info.InfoEndpoint", scope)
        )
    }

    fun testDefaultConfigurationExposesHealthAloneButListsTheOthers() {
        assertEquals(EXPOSED, exposureOf("/actuator/health"))
        assertEquals(NOT_EXPOSED, exposureOf("/actuator/info"))
        assertEquals(NOT_EXPOSED, exposureOf("/actuator/env"))
    }

    fun testIncludeListExposesTheListedEndpointsOnly() {
        myFixture.addFileToProject(
            "application.yaml",
            """
            management:
              endpoints:
                web:
                  exposure:
                    include: health,info,metrics
            """.trimIndent()
        )

        assertEquals(EXPOSED, exposureOf("/actuator/health"))
        assertEquals(EXPOSED, exposureOf("/actuator/info"))
        assertEquals(NOT_EXPOSED, exposureOf("/actuator/env"))
    }

    fun testYamlSequenceIncludeIsRead() {
        myFixture.addFileToProject(
            "application.yaml",
            """
            management:
              endpoints:
                web:
                  exposure:
                    include:
                      - info
                      - env
            """.trimIndent()
        )

        assertEquals(EXPOSED, exposureOf("/actuator/info"))
        assertEquals(EXPOSED, exposureOf("/actuator/env"))
        assertEquals("a listed include replaces the default", NOT_EXPOSED, exposureOf("/actuator/health"))
    }

    fun testStarWithExclusionExposesEverythingButTheExcludedOne() {
        myFixture.addFileToProject(
            "application.properties",
            """
            management.endpoints.web.exposure.include=*
            management.endpoints.web.exposure.exclude=env
            """.trimIndent()
        )

        assertEquals(NOT_EXPOSED, exposureOf("/actuator/env"))
        assertEquals(EXPOSED, exposureOf("/actuator/health"))
        assertEquals(EXPOSED, exposureOf("/actuator/info"))
    }

    fun testBasePathAndPathMappingApplyToBuiltInEndpoints() {
        myFixture.addFileToProject(
            "application.properties",
            """
            management.endpoints.web.base-path=/manage
            management.endpoints.web.path-mapping.health=healthcheck
            """.trimIndent()
        )

        val paths = actuatorEndpoints().map { it.path }.toSet()
        assertTrue("health is moved to its mapped path, got $paths", "/manage/healthcheck" in paths)
        assertFalse(paths.any { it.startsWith("/actuator") })
    }

    fun testUnresolvablePlaceholderInIncludeLeavesTheExposureUnknown() {
        myFixture.addFileToProject(
            "application.properties",
            "management.endpoints.web.exposure.include=health,\${extra.endpoints}"
        )

        assertEquals(EXPOSED, exposureOf("/actuator/health"))
        assertEquals(UNKNOWN, exposureOf("/actuator/env"))
    }

    fun testResolvedPlaceholderInIncludeIsRead() {
        myFixture.addFileToProject(
            "application.properties",
            """
            extra.endpoints=env
            management.endpoints.web.exposure.include=health,${'$'}{extra.endpoints}
            """.trimIndent()
        )

        assertEquals(EXPOSED, exposureOf("/actuator/env"))
    }

    fun testProjectEndpointRedeclaringABuiltInIdReplacesIt() {
        myFixture.addFileToProject(
            "CustomHealthEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "health")
            class CustomHealthEndpoint {
                @ReadOperation
                fun health() = "UP"
            }
            """.trimIndent()
        )

        val health = actuatorEndpoints().filter { it.path == "/actuator/health" }
        assertEquals(listOf("CustomHealthEndpoint"), health.map { it.containingClass?.name }.distinct())
        assertEquals(1, health.size)
    }

    fun testBuiltInEndpointsAreNotSourcesOfSynthesizedKeys() {
        assertFalse(
            "built-in ids ship their own metadata, so key synthesis must not see them",
            "health" in ActuatorEndpointKeys.endpointsById(module)
        )
        assertTrue("health" in ActuatorEndpointKeys.libraryEndpointsById(module))
    }

    fun testProjectEndpointIsNotReportedAsALibraryEndpoint() {
        myFixture.addFileToProject(
            "CacheStatsEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "cachestats")
            class CacheStatsEndpoint {
                @ReadOperation
                fun stats() = "ok"
            }
            """.trimIndent()
        )

        assertTrue("cachestats" in ActuatorEndpointKeys.endpointsById(module))
        assertFalse(
            "a project endpoint is the project's, not a library's",
            "cachestats" in ActuatorEndpointKeys.libraryEndpointsById(module)
        )
        assertEquals(1, actuatorEndpoints().count { it.path == "/actuator/cachestats" })
    }

    fun testToolWindowShowsANotExposedEndpointAsSuch() {
        val health = JavaPsiFacade.getInstance(project).findClass(
            "org.springframework.boot.health.actuate.endpoint.HealthEndpoint", GlobalSearchScope.allScope(project)
        )!!
        fun renderedText(exposure: EndpointExposure?): String {
            val viewData = EndpointElementViewData(
                EndpointType.ACTUATOR, SmartPointerManager.createPointer<PsiElement>(health),
                "HealthEndpoint", "GET", "/actuator/health", exposure
            )
            val node = HttpMethodNode(viewData, RootEndpointNode(emptyList()))
            node.update()
            return node.presentation.coloredText.joinToString("") { it.text }
        }

        assertEquals("/actuator/health  ${SpringWebBundle.message("explyt.web.endpoints.tool.actuator.not.exposed")}", renderedText(NOT_EXPOSED))
        assertEquals("/actuator/health  ${SpringWebBundle.message("explyt.web.endpoints.tool.actuator.exposure.unknown")}", renderedText(UNKNOWN))
        assertFalse(
            "an exposed endpoint carries no note",
            renderedText(EXPOSED).contains(SpringWebBundle.message("explyt.web.endpoints.tool.actuator.not.exposed"))
        )
    }

    fun testUrlLiteralResolvesToABuiltInEndpoint() {
        val matched = SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .flatMap { it.getEndpointElements("/actuator/health", module) }
            .toList()

        assertEquals(listOf("/actuator/health"), matched.map { it.path }.distinct())
    }

    fun testJmxOnlyBuiltInsAreStillNotListed() {
        val paths = actuatorEndpoints().map { it.path }
        assertFalse("no endpoint gets a path from a JMX-only declaration, got $paths", paths.any { it.contains("jmx") })
    }

    private fun exposureOf(path: String): EndpointExposure {
        val matching = actuatorEndpoints().filter { it.path == path }
        assertFalse("expected $path among ${actuatorEndpoints().map { it.path }.distinct().sorted()}", matching.isEmpty())
        return matching.map { it.exposure }.distinct().single() ?: error("$path carries no exposure")
    }

    private fun actuatorEndpoints(): List<EndpointElement> =
        SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .toList()
}
