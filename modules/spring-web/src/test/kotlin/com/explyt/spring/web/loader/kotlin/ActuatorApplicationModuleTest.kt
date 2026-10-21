/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.core.SpringCoreClasses.ACTUATOR_ENDPOINT_AUTO_CONFIGURATION
import com.explyt.spring.core.properties.references.ActuatorEndpointKeys
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
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
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

    fun testEnableAutoConfigurationApplicationListsTheBuiltIns() {
        addFileToModule(
            module, "com/example/portal/PortalApplication.kt",
            "package com.example.portal\n\n" +
                    "import org.springframework.boot.autoconfigure.EnableAutoConfiguration\n" +
                    "import org.springframework.context.annotation.Configuration\n\n" +
                    "@Configuration\n@EnableAutoConfiguration\nclass PortalApplication\n"
        )
        addFileToModule(module, "application.yml", INCLUDE_INFO)
        val portal = JavaPsiFacade.getInstance(project)
            .findClass("com.example.portal.PortalApplication", GlobalSearchScope.projectScope(project))
        assertNotNull("precondition: PortalApplication resolves", portal)
        assertTrue(
            "precondition: PortalApplication carries Boot's EnableAutoConfiguration",
            portal!!.hasAnnotation("org.springframework.boot.autoconfigure.EnableAutoConfiguration")
        )

        val info = actuatorAt("/actuator/info")
        assertEquals("info is listed once, by the application: ${describe(info)}", 1, info.size)
        assertEquals(EXPOSED, info.single().exposure)
        assertEquals(portal, info.single().application)
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

    /**
     * The Spring Boot petclinic sample declares `spring-boot-starter-actuator` as `runtimeOnly`: the Actuator jars are
     * on the application's runtime classpath and on no compile scope, yet the running application serves every
     * built-in endpoint.
     */
    fun testRuntimeOnlyActuatorStarterListsTheBuiltIns() {
        val application = addActuatorModule("petclinic", DependencyScope.RUNTIME)
        addApplication(application, "petclinic", null)
        addFileToModule(application, "application.properties", EXPOSE_ALL)
        val facade = JavaPsiFacade.getInstance(project)
        assertNull(
            "precondition: the compile classpath has no Actuator auto-configuration",
            facade.findClass(ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, GlobalSearchScope.moduleWithLibrariesScope(application))
        )
        assertNotNull(
            "precondition: the runtime classpath has it",
            facade.findClass(ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, application.getModuleRuntimeScope(false))
        )

        val health = actuatorAt("/actuator/health")
        assertEquals("health is listed once, by the runtime-only application: ${describe(health)}", 1, health.size)
        assertEquals(EXPOSED, health.single().exposure)
        assertEquals(EndpointAccess.UNRESTRICTED, health.single().access)

        val shutdown = actuatorAt("/actuator/shutdown").filter { "POST" in it.requestMethods }
        assertEquals("shutdown is listed once: ${describe(shutdown)}", 1, shutdown.size)
        assertEquals(EXPOSED, shutdown.single().exposure)
        assertEquals("its declared defaultAccess is read", EndpointAccess.NONE, shutdown.single().access)
    }

    /**
     * A Gradle `test` source-set module declaring a test application is a context of its own, so it lists the built-ins
     * too - each copy carrying the application listing it.
     */
    fun testTestSourceApplicationCopyCarriesItsApplication() {
        val main = addActuatorModule("petclinic", DependencyScope.RUNTIME)
        addApplication(main, "petclinic", null)
        addFileToModule(main, "application.properties", EXPOSE_ALL)
        val tests = addUnrelatedModule("petclinicTest")
        ModuleRootModificationUtil.addDependency(tests, main)
        addTestApplication(tests, "crash")
        val facade = JavaPsiFacade.getInstance(project)
        for (each in listOf(main, tests)) {
            assertNotNull(
                "precondition: ${each.name} sees the Actuator auto-configuration at runtime",
                facade.findClass(ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, each.getModuleRuntimeScope(false))
            )
        }
        val production = applicationClass("petclinic")
        val test = applicationClass("crash")
        assertFalse("precondition: production app", isInTestSources(production))
        assertTrue("precondition: test app", isInTestSources(test))

        val health = actuatorAt("/actuator/health")
        assertEquals("health is listed by both applications: ${describe(health)}", 2, health.size)
        assertEquals(setOf(production, test), health.map { it.application }.toSet())
    }

    fun testModuleWithProductionAndTestApplicationsListsForTheProductionOne() {
        val app = addActuatorModule("petclinic", DependencyScope.RUNTIME)
        addFileToModule(app, "application.properties", EXPOSE_ALL)
        addTestApplication(app, "aaa")
        addApplication(app, "zzz", null)

        val health = actuatorAt("/actuator/health")
        assertEquals("health is listed once: ${describe(health)}", 1, health.size)
        assertEquals(applicationClass("zzz"), health.single().application)
    }

    fun testModuleWithTwoProductionApplicationsPicksByQualifiedName() {
        val app = addActuatorModule("petclinic", DependencyScope.RUNTIME)
        addFileToModule(app, "application.properties", EXPOSE_ALL)
        addApplication(app, "zzz", null)
        addApplication(app, "aaa", null)

        val health = actuatorAt("/actuator/health")
        assertEquals("health is listed once: ${describe(health)}", 1, health.size)
        assertEquals(applicationClass("aaa"), health.single().application)
    }

    fun testTestScopedActuatorListsNoBuiltIns() {
        val application = addActuatorModule("tested", DependencyScope.TEST)
        addApplication(application, "tested", null)
        addFileToModule(application, "application.properties", EXPOSE_ALL)
        val facade = JavaPsiFacade.getInstance(project)
        assertNotNull(
            "precondition: the test classpath has the Actuator auto-configuration",
            facade.findClass(ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, application.getModuleRuntimeScope(true))
        )
        assertNull(
            "precondition: the production runtime classpath has not",
            facade.findClass(ACTUATOR_ENDPOINT_AUTO_CONFIGURATION, application.getModuleRuntimeScope(false))
        )

        assertEquals(emptyMap<String, Any>(), ActuatorEndpointKeys.libraryEndpointsById(application))
        assertEmpty(actuatorAt("/actuator/health"))
    }

    private fun addActuatorModule(name: String, actuatorScope: DependencyScope): Module {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val application = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)
        ModuleRootModificationUtil.setSdkInherited(application)
        ModuleRootModificationUtil.updateModel(application) { model ->
            addFromMaven(model, BOOT_AUTO_CONFIGURE.mavenCoordinates, BOOT_AUTO_CONFIGURE.includeTransitiveDependencies)
            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies, actuatorScope)
            }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val scopes = ModuleRootManager.getInstance(application).orderEntries
            .filterIsInstance<LibraryOrderEntry>()
            .associate { it.libraryName to it.scope }
        val expected = mapOf(BOOT_AUTO_CONFIGURE.mavenCoordinates to DependencyScope.COMPILE) +
                libraries.associate { it.mavenCoordinates to actuatorScope }
        assertEquals("precondition: the Actuator libraries are $actuatorScope-scoped", expected, scopes)
        return application
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

    private fun addTestApplication(target: Module, name: String) {
        addTestSourceFileToModule(
            target, "com/example/$name/Application.kt",
            "package com.example.$name\n\n" +
                    "import org.springframework.boot.autoconfigure.SpringBootApplication\n\n" +
                    "@SpringBootApplication\nclass Application\n"
        )
    }

    private fun applicationClass(name: String): PsiClass =
        JavaPsiFacade.getInstance(project).findClass("com.example.$name.Application", GlobalSearchScope.projectScope(project))
            ?: error("no application $name")

    private fun isInTestSources(psiClass: PsiClass): Boolean =
        ProjectFileIndex.getInstance(project).isInTestSourceContent(psiClass.containingFile.virtualFile)

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
        const val EXPOSE_ALL = "management.endpoints.web.exposure.include=*\n"
        val BOOT_AUTO_CONFIGURE = TestLibrary.springBootAutoConfigure_4_1_0
    }
}
