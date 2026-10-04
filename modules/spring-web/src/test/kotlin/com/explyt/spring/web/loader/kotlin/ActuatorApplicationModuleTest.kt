/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointAccess
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointExposure.EXPOSED
import com.explyt.spring.web.loader.EndpointExposure.NOT_EXPOSED
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.test.addFromMaven
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil

/**
 * Actuator endpoints belong to the context an application starts: they are listed by the module declaring the
 * application, under its configuration, and by no module that merely has the Actuator jar or declares an endpoint
 * the application reaches.
 */
class ActuatorApplicationModuleTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0, TestLibrary.springBootHealth_4_1_0)

    fun testLibraryModuleWithoutConfigurationListsNoUnexposedCopy() {
        addApplication(module, "app", INCLUDE_INFO)
        addDependencyModule("library")

        val info = actuatorAt("/actuator/info")
        assertEquals("info is listed once, by the application: ${describe(info)}", 1, info.size)
        assertEquals(EXPOSED, info.single().exposure)
    }

    fun testNoApplicationListsNoBuiltIns() {
        addDependencyModule("library")

        assertEmpty(actuatorAt("/actuator/health"))
    }

    fun testTwoApplicationsListTheirOwnVerdicts() {
        addApplication(module, "app", INCLUDE_INFO)
        addApplication(addUnrelatedModule("second"), "second", null)

        val info = actuatorAt("/actuator/info")
        assertEquals("one info per application: ${describe(info)}", 2, info.size)
        assertEquals(setOf(EXPOSED, NOT_EXPOSED), info.map { it.exposure }.toSet())
    }

    fun testTwoApplicationsWithTheSameVerdictAreNotCollapsed() {
        addApplication(module, "app", null)
        addApplication(addUnrelatedModule("second"), "second", null)

        val health = actuatorAt("/actuator/health")
        assertEquals("one health per application: ${describe(health)}", 2, health.size)
    }

    fun testAccessFollowsTheApplication() {
        addApplication(module, "app", SHUTDOWN_UNRESTRICTED)
        addDependencyModule("library")

        val shutdown = actuatorAt("/actuator/shutdown").filter { "POST" in it.requestMethods }
        assertEquals("shutdown is listed once: ${describe(shutdown)}", 1, shutdown.size)
        assertEquals(EndpointAccess.UNRESTRICTED, shutdown.single().access)
    }

    /**
     * A module depending on the application - integration tests, say - sees the application class through its
     * dependencies, yet declares no application: the built-ins stay the application's, listed once.
     */
    fun testModuleDependingOnTheApplicationListsNoBuiltIns() {
        addApplication(module, "app", INCLUDE_INFO)
        addDependentModule("integration")

        val info = actuatorAt("/actuator/info")
        assertEquals("info is listed once, by the application: ${describe(info)}", 1, info.size)
        assertEquals(EXPOSED, info.single().exposure)
    }

    fun testLibraryEndpointFollowsTheApplicationReachingIt() {
        addApplication(module, "app", INCLUDE_CUSTOM)
        addCustomEndpoint(addDependencyModule("library"))

        val custom = actuatorAt("/actuator/custom")
        assertEquals("custom is listed once, by the application: ${describe(custom)}", 1, custom.size)
        assertEquals(EXPOSED, custom.single().exposure)
    }

    fun testTwoApplicationsReachingOneLibraryEndpointListTheirOwnVerdicts() {
        addApplication(module, "app", INCLUDE_CUSTOM)
        val library = addDependencyModule("library")
        addCustomEndpoint(library)
        val second = addUnrelatedModule("second")
        ModuleRootModificationUtil.addDependency(second, library)
        addApplication(second, "second", null)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val custom = actuatorAt("/actuator/custom")
        assertEquals("one custom per application: ${describe(custom)}", 2, custom.size)
        assertEquals(setOf(EXPOSED, NOT_EXPOSED), custom.map { it.exposure }.toSet())
    }

    /**
     * The same project declaration under the same verdict is still one endpoint per application: what makes them two is
     * the application listing each, not a difference in what the configuration decided.
     */
    fun testTwoApplicationsWithTheSameVerdictOnOneLibraryEndpointAreNotCollapsed() {
        addApplication(module, "app", INCLUDE_CUSTOM)
        val library = addDependencyModule("library")
        addCustomEndpoint(library)
        val second = addUnrelatedModule("second")
        ModuleRootModificationUtil.addDependency(second, library)
        addApplication(second, "second", INCLUDE_CUSTOM)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val custom = actuatorAt("/actuator/custom")
        assertEquals("one custom per application: ${describe(custom)}", 2, custom.size)
        assertEquals(setOf(EXPOSED), custom.map { it.exposure }.toSet())
    }

    fun testLibraryEndpointWithoutApplicationFollowsItsOwnConfiguration() {
        val library = addDependencyModule("library")
        addCustomEndpoint(library)
        addFileToModule(library, "application.yml", INCLUDE_CUSTOM)

        val custom = actuatorAt("/actuator/custom")
        assertEquals("custom is listed once, by its library: ${describe(custom)}", 1, custom.size)
        assertEquals(EXPOSED, custom.single().exposure)
    }

    private fun addCustomEndpoint(target: Module) {
        addFileToModule(
            target, "com/example/library/CustomEndpoint.kt",
            "package com.example.library\n\n" +
                    "import org.springframework.boot.actuate.endpoint.annotation.Endpoint\n" +
                    "import org.springframework.boot.actuate.endpoint.annotation.ReadOperation\n\n" +
                    "@Endpoint(id = \"custom\")\nclass CustomEndpoint {\n" +
                    "    @ReadOperation\n    fun read(): String = \"custom\"\n}\n"
        )
    }

    private fun addApplication(target: Module, name: String, configuration: String?) {
        addFileToModule(
            target, "com/example/$name/Application.kt",
            "package com.example.$name\n\n" +
                    "import org.springframework.boot.autoconfigure.SpringBootApplication\n\n" +
                    "@SpringBootApplication\nclass Application\n"
        )
        configuration?.let { addFileToModule(target, "application.yml", it) }
    }

    /** A second application beside the first, neither depending on the other: each reads only its own configuration. */
    private fun addUnrelatedModule(name: String): Module {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val unrelated = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)
        ModuleRootModificationUtil.setSdkInherited(unrelated)
        ModuleRootModificationUtil.updateModel(unrelated) { model ->
            libraries.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return unrelated
    }

    private fun actuatorAt(path: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .filter { it.type == EndpointType.ACTUATOR && it.path == path }

    private fun describe(endpoints: List<EndpointElement>) =
        endpoints.map { "${it.requestMethods} ${it.path} ${it.exposure} ${it.access}" }

    private companion object {
        const val INCLUDE_INFO = "management:\n  endpoints:\n    web:\n      exposure:\n        include: health,info\n"
        const val SHUTDOWN_UNRESTRICTED = "management:\n  endpoint:\n    shutdown:\n      access: unrestricted\n"
        const val INCLUDE_CUSTOM = "management:\n  endpoints:\n    web:\n      exposure:\n        include: health,custom\n"
    }
}
