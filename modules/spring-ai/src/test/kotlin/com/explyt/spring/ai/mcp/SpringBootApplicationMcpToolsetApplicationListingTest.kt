/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PsiTestUtil

class SpringBootApplicationMcpToolsetApplicationListingTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private lateinit var testSourceRoot: VirtualFile

    override fun setUp() {
        super.setUp()
        testSourceRoot = myFixture.tempDirFixture.findOrCreateDir("src/test/java")
        PsiTestUtil.addSourceRoot(module, testSourceRoot, true)
    }

    override fun tearDown() {
        try {
            PsiTestUtil.removeSourceRoot(module, testSourceRoot)
        } finally {
            super.tearDown()
        }
    }

    fun testListsProductionAndTestSpringBootApplicationsWithTestSourceMarker() = runBlockingReleasingWriteIntent<Unit> {
        addJava("com/example/ProdApp.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ProdApp {}
        """)
        addJava("src/test/java/com/example/TestApp.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class TestApp {}
        """)

        val rows = applications()

        assertEquals(listOf("com.example.ProdApp", "com.example.TestApp"), rows.map { it["fullyQualifiedClassName"].asText() })
        assertEquals(PRODUCTION_KEYS, keys(rows[0]))
        assertTrue("test application must be marked, got ${rows[1]}", rows[1]["testSource"].asBoolean())
    }

    fun testListsTestSourceEnableAutoConfigurationApplicationAfterProduction() = runBlockingReleasingWriteIntent<Unit> {
        addJava("com/example/ProdApp.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ProdApp {}
        """)
        addJava("src/test/java/com/example/TestConfigApp.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableAutoConfiguration
            public class TestConfigApp {}
        """)

        val rows = applications()

        assertEquals(listOf("com.example.ProdApp", "com.example.TestConfigApp"), rows.map { it["fullyQualifiedClassName"].asText() })
        assertTrue("test EAC application must be marked, got ${rows[1]}", rows[1]["testSource"].asBoolean())
    }

    fun testDoesNotListMetaAnnotationTypeAsApplication() = runBlockingReleasingWriteIntent<Unit> {
        addJava("com/example/MyApp.java", """
            package com.example;

            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            @Retention(RetentionPolicy.RUNTIME)
            public @interface MyApp {}
        """)
        addJava("com/example/App.java", """
            package com.example;

            @MyApp
            public class App {}
        """)

        val rows = applications()

        assertEquals(listOf("com.example.App"), rows.map { it["fullyQualifiedClassName"].asText() })
    }

    fun testOrdersProductionApplicationsByQualifiedNameBeforeTestApplications() = runBlockingReleasingWriteIntent<Unit> {
        addJava("com/zeta/ProdApp.java", """
            package com.zeta;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ProdApp {}
        """)
        addJava("com/alpha/ProdApp.java", """
            package com.alpha;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class ProdApp {}
        """)
        addJava("src/test/java/com/zero/TestApp.java", """
            package com.zero;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class TestApp {}
        """)

        assertEquals(
            listOf("com.alpha.ProdApp", "com.zeta.ProdApp", "com.zero.TestApp"),
            applications().map { it["fullyQualifiedClassName"].asText() }
        )
    }

    fun testListsProductionEnableAutoConfigurationApplication() = runBlockingReleasingWriteIntent<Unit> {
        addJava("com/example/ConfigApp.java", """
            package com.example;

            import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @EnableAutoConfiguration
            public class ConfigApp {}
        """)

        assertEquals(listOf("com.example.ConfigApp"), applications().map { it["fullyQualifiedClassName"].asText() })
    }

    private fun addJava(path: String, source: String) {
        myFixture.addFileToProject(path, source.trimIndent())
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private suspend fun applications(): List<JsonNode> =
        mapper.readTree(toolset.getAllSpringBootApplications(project.basePath)).toList()

    private fun keys(row: JsonNode): Set<String> = row.fieldNames().asSequence().toSet()

    private companion object {
        val PRODUCTION_KEYS = setOf(
            "fullyQualifiedClassName", "springBootVersion", "springBootStarters", "moduleName", "buildTool"
        )
    }
}
