/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.explyt.spring.web.util.ApplicationBasePath
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.readAction
import com.intellij.openapi.module.JavaModuleType
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil
import kotlinx.coroutines.runBlocking

/**
 * A base path belongs to one application: the lookup spans every module of the project, and each module's
 * configuration declares its own. Stripping one module's context path and then matching another module's route
 * reported a base path under which that route is not served.
 */
class SpringBootApplicationMcpToolsetBasePathModulesTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testBasePathOfOneModuleDoesNotAdmitTheRoutesOfAnother() = runBlocking<Unit> {
        val admin = addApplicationModule("admin")
        addFileToModule(admin, "application.yml", "server:\n  servlet:\n    context-path: /admin\n")
        addFileToModule(admin, "com/example/admin/AdminController.java", controller("com.example.admin", "/console"))
        addFileToModule(module, "com/example/shop/ItemController.java", controller("com.example.shop", "/items"))

        assertEquals("/admin", readAction { ApplicationBasePath.of(admin) })
        assertNull("The shop module declares no base path", readAction { ApplicationBasePath.of(module) })

        val underAdmin = find("/admin/console/7")
        assertEquals(listOf("/console/{id}"), paths(underAdmin["endpoints"]))
        assertEquals("/admin", underAdmin["basePath"].asText())

        val shopUnderAdmin = find("/admin/items/7")
        assertTrue(
            "The shop is not served under the admin context path, got $shopUnderAdmin",
            shopUnderAdmin["basePath"].isNull
        )
        assertEquals(
            "Only a guess can still reach the shop route, and it is reported as one",
            "/admin", shopUnderAdmin["assumedPrefix"].asText()
        )
    }

    private suspend fun find(url: String): JsonNode =
        mapper.readTree(toolset.findEndpoint(urlPattern = url, projectPath = project.basePath, httpMethod = ""))

    private fun paths(nodes: JsonNode): List<String> = nodes.map { it["fullPath"].asText() }

    private fun addApplicationModule(name: String): Module {
        val sourceRoot = myFixture.tempDirFixture.findOrCreateDir("$name/src")
        val application = PsiTestUtil.addModule(project, JavaModuleType.getModuleType(), name, sourceRoot)
        ModuleRootModificationUtil.setSdkInherited(application)
        ModuleRootModificationUtil.updateModel(application) { model ->
            libraries.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        return application
    }

    private fun controller(packageName: String, prefix: String): String = """
        package $packageName;

        import org.springframework.web.bind.annotation.GetMapping;
        import org.springframework.web.bind.annotation.PathVariable;
        import org.springframework.web.bind.annotation.RequestMapping;
        import org.springframework.web.bind.annotation.RestController;

        @RestController
        @RequestMapping("$prefix")
        public class ${if (prefix == "/console") "AdminController" else "ItemController"} {
            @GetMapping("/{id}")
            public String get(@PathVariable("id") String id) {
                return id;
            }
        }
    """.trimIndent()
}
