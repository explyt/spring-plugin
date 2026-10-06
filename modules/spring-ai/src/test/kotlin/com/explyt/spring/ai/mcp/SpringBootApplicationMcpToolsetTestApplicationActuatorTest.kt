/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.roots.ProjectFileIndex
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import kotlinx.coroutines.runBlocking

/**
 * A Gradle `test` source-set module declaring a test application lists every built-in Actuator endpoint a second
 * time: the copy names its application and is marked as test code, after the production copy.
 */
class SpringBootApplicationMcpToolsetTestApplicationActuatorTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0, TestLibrary.springBootHealth_4_1_0)

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        val main = addActuatorModule("petclinic")
        addApplication(main)
        addFileToModule(main, "application.properties", "management.endpoints.web.exposure.include=*\n")
        val tests = addActuatorModule("petclinicTest")
        ModuleRootModificationUtil.addDependency(tests, main)
        addTestSourceFileToModule(
            tests, "com/example/crash/CrashApplication.kt",
            "package com.example.crash\n\n" +
                    "import org.springframework.boot.autoconfigure.SpringBootApplication\n\n" +
                    "@SpringBootApplication\nclass CrashApplication\n"
        )
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        assertFalse("precondition: production app", isInTestSources(PRODUCTION))
        assertTrue("precondition: test app", isInTestSources(TEST))
    }

    fun testListingMarksTheTestApplicationCopy() = runBlocking<Unit> {
        val listed = mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), endpointType = "ACTUATOR", compact = true)
        )["endpoints"].toList()

        assertTestCopyFollows(listed)
    }

    fun testFindMarksTheTestApplicationCopy() = runBlocking<Unit> {
        val found = mapper.readTree(
            toolset.findEndpoint(urlPattern = "/actuator/health", projectPath = projectPath())
        )["endpoints"].toList()

        assertTestCopyFollows(found)
    }

    private fun assertTestCopyFollows(records: List<JsonNode>) {
        val health = records.filter { it["fullPath"].asText() == "/actuator/health" }
        assertEquals("health is listed by both applications: $health", 2, health.size)
        val (production, test) = health
        assertEquals(PRODUCTION, production["application"]?.asText())
        assertFalse("the production copy is not test code: $production", production.has("testSource"))
        assertEquals(TEST, test["application"]?.asText())
        assertTrue("the test copy is test code: $test", test["testSource"]?.asBoolean() == true)
        val firstTest = records.indexOfFirst { it.has("testSource") }
        assertTrue(
            "every production record precedes the test copies: $records",
            records.drop(firstTest).all { it["testSource"]?.asBoolean() == true }
        )
    }

    private fun addActuatorModule(name: String): Module {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val module = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)
        ModuleRootModificationUtil.setSdkInherited(module)
        ModuleRootModificationUtil.updateModel(module) { model ->
            val autoConfigure = TestLibrary.springBootAutoConfigure_4_1_0
            addFromMaven(model, autoConfigure.mavenCoordinates, autoConfigure.includeTransitiveDependencies)
            libraries.forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies, DependencyScope.RUNTIME)
            }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return module
    }

    private fun addApplication(target: Module) {
        addFileToModule(
            target, "com/example/petclinic/PetClinicApplication.kt",
            "package com.example.petclinic\n\n" +
                    "import org.springframework.boot.autoconfigure.SpringBootApplication\n\n" +
                    "@SpringBootApplication\nclass PetClinicApplication\n"
        )
    }

    private fun isInTestSources(qualifiedName: String): Boolean {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))
            ?: error("no class $qualifiedName")
        return ProjectFileIndex.getInstance(project).isInTestSourceContent(psiClass.containingFile.virtualFile)
    }

    private fun projectPath(): String = project.basePath ?: ""

    private companion object {
        const val PRODUCTION = "com.example.petclinic.PetClinicApplication"
        const val TEST = "com.example.crash.CrashApplication"
    }
}
