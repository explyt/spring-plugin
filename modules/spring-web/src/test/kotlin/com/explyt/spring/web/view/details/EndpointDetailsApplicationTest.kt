/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.view.details

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.service.SpringWebEndpointsSearcher
import com.explyt.spring.web.view.EndpointsTreeData
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.IndexingTestUtil

class EndpointDetailsApplicationTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0, TestLibrary.springBootHealth_4_1_0)

    fun testLibraryEndpointOfTwoApplicationsResolvesToTheApplicationOfItsRow() {
        addApplication(module, "app", INCLUDE_CUSTOM)
        val library = addDependencyModule("library")
        addCustomEndpoint(library)
        val second = addStandaloneModule("second")
        ModuleRootModificationUtil.addDependency(second, library)
        addApplication(second, "second", null)
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val rows = SpringWebEndpointsSearcher.getInstance(project).getAllEndpoints()
            .filter { it.type == EndpointType.ACTUATOR && it.path == "/actuator/custom" }
            .flatMap(EndpointsTreeData::rowsOf)
        assertEquals(
            "Precondition: one row per application",
            setOf("com.example.app.Application", "com.example.second.Application"),
            rows.map { it.application }.toSet()
        )
        assertEquals("Precondition: the rows share one operation", 1, rows.map { it.psiPointer.element }.distinct().size)
        assertEquals("Precondition: the rows share one class", 1, rows.map { it.classOrFileName }.distinct().size)

        rows.forEach { row ->
            assertEquals(row.application, EndpointDetailsReader.endpointOf(row, project)?.application?.qualifiedName)
        }
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

    private companion object {
        const val INCLUDE_CUSTOM = "management:\n  endpoints:\n    web:\n      exposure:\n        include: health,custom\n"
    }
}
