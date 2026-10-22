/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil

class SpringBootApplicationMcpToolsetApplicationListingKotlinTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private lateinit var testSourceRoot: VirtualFile

    override fun setUp() {
        super.setUp()
        testSourceRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/kotlin")
        PsiTestUtil.addSourceRoot(module, testSourceRoot, true)
    }

    override fun tearDown() {
        try {
            PsiTestUtil.removeSourceRoot(module, testSourceRoot)
        } finally {
            super.tearDown()
        }
    }

    fun testListsKotlinProductionAndTestApplicationsInApplicationClassesOrder() = runBlockingReleasingWriteIntent<Unit> {
        addKotlin("com/example/ZetaApp.kt", """
            package com.example

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class ZetaApp
        """)
        addKotlin("src/test/kotlin/com/example/AlphaTestApp.kt", """
            package com.example

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class AlphaTestApp
        """)

        val rows = applications()

        assertEquals(listOf("com.example.ZetaApp", "com.example.AlphaTestApp"), rows.map { it["fullyQualifiedClassName"].asText() })
        assertTrue("test application must be marked, got ${rows[1]}", rows[1]["testSource"].asBoolean())
    }

    fun testDoesNotListKotlinMetaAnnotationTypeAsApplication() = runBlockingReleasingWriteIntent<Unit> {
        addKotlin("com/example/MyApp.kt", """
            package com.example

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            annotation class MyApp
        """)
        addKotlin("com/example/App.kt", """
            package com.example

            @MyApp
            class App
        """)

        assertEquals(listOf("com.example.App"), applications().map { it["fullyQualifiedClassName"].asText() })
    }

    private fun addKotlin(path: String, source: String) {
        myFixture.addFileToProject(path, source.trimIndent())
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private suspend fun applications(): List<JsonNode> = mapper.readTree(
        toolset.getAllSpringBootApplications(project.basePath)
    ).toList()
}
