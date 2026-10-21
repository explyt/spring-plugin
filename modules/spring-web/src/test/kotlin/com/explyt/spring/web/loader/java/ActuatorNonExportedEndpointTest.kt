/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.core.SpringCoreClasses.ACTUATOR_ENDPOINT
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointElement
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiPolyVariantReference
import com.intellij.psi.search.GlobalSearchScope
import org.intellij.lang.annotations.Language

class ActuatorNonExportedEndpointTest : ExplytMultiModuleTestCase() {

    private lateinit var applicationProperties: PsiFile

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)

    fun testNonExportedLibraryEndpointIsDiscoveredForTheApplication() {
        val lib = addActuatorLibraryLayout(actuatorExported = false)
        assertActuatorOutsideApplicationCompileScope(lib)

        assertTrue(
            "the application's endpoints include custom: ${ActuatorEndpointKeys.endpointsById(module).keys}",
            "custom" in ActuatorEndpointKeys.endpointsById(module)
        )
    }

    fun testNonExportedLibraryEndpointIsListedByTheApplication() {
        val lib = addActuatorLibraryLayout(actuatorExported = false)
        assertActuatorOutsideApplicationCompileScope(lib)

        val custom = actuatorAt("/actuator/custom")
        assertEquals("custom is listed once, by the application: ${describe(custom)}", 1, custom.size)
        assertEquals(applicationClass(), custom.single().application)
    }

    fun testNonExportedLibraryEndpointKeyNavigatesToTheEndpoint() {
        val lib = addActuatorLibraryLayout(actuatorExported = false)
        assertActuatorOutsideApplicationCompileScope(lib)

        assertEquals(listOf("CustomEndpoint"), endpointIdTargets().map { (it as? PsiClass)?.name })
    }

    fun testExportedLibraryEndpointIsListedByTheApplication() {
        addActuatorLibraryLayout(actuatorExported = true)
        assertNotNull(
            "precondition: the exported Actuator reaches the application's compile classpath",
            findClass(ACTUATOR_ENDPOINT, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
        )

        val custom = actuatorAt("/actuator/custom")
        assertEquals("custom is listed once, by the application: ${describe(custom)}", 1, custom.size)
        assertEquals(applicationClass(), custom.single().application)
    }

    fun testExportedLibraryEndpointKeyNavigatesToTheEndpoint() {
        addActuatorLibraryLayout(actuatorExported = true)

        assertEquals(listOf("CustomEndpoint"), endpointIdTargets().map { (it as? PsiClass)?.name })
    }

    fun testLibraryWithoutApplicationListsItsOwnEndpoint() {
        addLibraries(module, ACTUATOR)
        addFileToModule(module, "com/example/lib/CustomEndpoint.java", CUSTOM_ENDPOINT)
        addFileToModule(module, "application.properties", EXPOSE_CUSTOM)
        assertNull(
            "precondition: no application",
            findClass(APPLICATION, GlobalSearchScope.projectScope(project))
        )

        val custom = actuatorAt("/actuator/custom")
        assertEquals("custom is listed once, by its library: ${describe(custom)}", 1, custom.size)
        assertNull(custom.single().application)
    }

    private fun addActuatorLibraryLayout(actuatorExported: Boolean): Module {
        val lib = addDependencyModule("lib", exported = false)
        addLibraries(lib, ACTUATOR, DependencyScope.COMPILE, actuatorExported)
        addLibraries(module, ACTUATOR, DependencyScope.RUNTIME)
        addFileToModule(lib, "com/example/lib/CustomEndpoint.java", CUSTOM_ENDPOINT)
        addFileToModule(module, "com/example/app/Application.java", APPLICATION_SOURCE)
        applicationProperties = addFileToModule(module, "application.properties", EXPOSE_CUSTOM)

        assertEquals(
            "precondition: lib's Actuator export flag",
            mapOf(ACTUATOR.single().mavenCoordinates to actuatorExported),
            actuatorEntriesOf(lib).associate { it.libraryName to it.isExported }
        )
        assertEquals(
            "precondition: the application has Actuator at runtime only",
            mapOf(ACTUATOR.single().mavenCoordinates to DependencyScope.RUNTIME),
            actuatorEntriesOf(module).associate { it.libraryName to it.scope }
        )
        assertNotNull(
            "precondition: @Endpoint resolves in lib",
            findClass(ACTUATOR_ENDPOINT, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(lib))
        )
        assertNotNull(
            "precondition: @Endpoint is on the application's runtime classpath",
            findClass(ACTUATOR_ENDPOINT, module.getModuleRuntimeScope(false))
        )
        assertNotNull("precondition: the application class", applicationClass())
        return lib
    }

    private fun assertActuatorOutsideApplicationCompileScope(lib: Module) {
        assertNull(
            "precondition: @Endpoint is absent from the application's compile classpath",
            findClass(ACTUATOR_ENDPOINT, GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module))
        )
        assertTrue(
            "precondition: lib declares custom in its own scope: ${ActuatorEndpointKeys.endpointsById(lib).keys}",
            "custom" in ActuatorEndpointKeys.endpointsById(lib)
        )
    }

    private fun actuatorEntriesOf(target: Module): List<LibraryOrderEntry> =
        ModuleRootManager.getInstance(target).orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .filter { it.libraryName in ACTUATOR.map(TestLibrary::mavenCoordinates) }

    private fun endpointIdTargets(): List<PsiElement> {
        myFixture.configureFromExistingVirtualFile(applicationProperties.virtualFile)
        val offset = myFixture.file.text.indexOf(ID_KEY_PREFIX) + ID_KEY_PREFIX.length + 1
        val reference = myFixture.file.findReferenceAt(offset) ?: return emptyList()
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference.resolve())
        }
    }

    private fun applicationClass(): PsiClass? = findClass(APPLICATION, GlobalSearchScope.projectScope(project))

    private fun findClass(name: String, scope: GlobalSearchScope): PsiClass? =
        JavaPsiFacade.getInstance(project).findClass(name, scope)

    private fun actuatorAt(path: String): List<EndpointElement> =
        SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .filter { it.type == EndpointType.ACTUATOR && it.path == path }

    private fun describe(endpoints: List<EndpointElement>) =
        endpoints.map { "${it.requestMethods} ${it.path} ${it.exposure} ${it.application?.qualifiedName}" }

    private companion object {
        val ACTUATOR = listOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)
        const val APPLICATION = "com.example.app.Application"
        const val ID_KEY_PREFIX = "management.endpoint."
        const val EXPOSE_CUSTOM =
            "management.endpoints.web.exposure.include=custom\nmanagement.endpoint.custom.cache.time-to-live=10s\n"

        @Language("java")
        val APPLICATION_SOURCE = """
            package com.example.app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class Application {
            }
        """.trimIndent()

        @Language("java")
        val CUSTOM_ENDPOINT = """
            package com.example.lib;

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

            @Endpoint(id = "custom")
            public class CustomEndpoint {
                @ReadOperation
                public String read() { return "custom"; }
            }
        """.trimIndent()
    }
}
