/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.McpSourceLocation
import com.explyt.spring.test.ExplytMultiModuleTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.openapi.roots.DependencyScope
import com.intellij.openapi.roots.TestSourcesFilter
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.intellij.lang.annotations.Language

/**
 * An injection point in a `@SpringBootTest` of a test-source application is answered from that application's
 * scan: a component only the test application reaches is a candidate there, and stays none for production code
 * (#772).
 */
class TestSourceInjectionPointBeanToolsTest : ExplytMultiModuleTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
    )

    private val mapper = ObjectMapper()

    override fun tearDown() {
        try {
            Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
        } finally {
            super.tearDown()
        }
    }

    fun testJavaTestClassInjectionPointFindsAComponentOnlyTheTestApplicationScans() = runBlockingReleasingWriteIntent {
        addFileToModule(module, "app/portal/PortalApplication.java", JAVA_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.java", JAVA_AUDIT)
        val consumer = addFileToModule(module, "app/portal/AuditConsumer.java", JAVA_AUDIT_CONSUMER)
        addTestSourceFileToModule(module, "app/LocalApp.java", JAVA_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.java", JAVA_AUDIT_IT)

        assertTestInjectionPointFindsTheAudit(auditIt, consumer, marker = "Audit audit;")
    }

    fun testKotlinTestClassInjectionPointFindsAComponentOnlyTheTestApplicationScans() = runBlockingReleasingWriteIntent {
        addFileToModule(module, "app/portal/PortalApplication.kt", KOTLIN_PORTAL_APPLICATION)
        addFileToModule(module, "app/audit/Audit.kt", KOTLIN_AUDIT)
        val consumer = addFileToModule(module, "app/portal/AuditConsumer.kt", KOTLIN_AUDIT_CONSUMER)
        addTestSourceFileToModule(module, "app/LocalApp.kt", KOTLIN_LOCAL_APP)
        val auditIt = addTestSourceFileToModule(module, "app/AuditIT.kt", KOTLIN_AUDIT_IT)

        assertTestInjectionPointFindsTheAudit(auditIt, consumer, marker = "lateinit var audit: Audit")
    }

    private suspend fun assertTestInjectionPointFindsTheAudit(auditIt: PsiFile, consumer: PsiFile, marker: String) {
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
        addLibraries(module, listOf(TestLibrary.springBootTest_3_2_3), DependencyScope.TEST)
        assertTrue(
            "Precondition: AuditIT must live in a test source root",
            TestSourcesFilter.isTestSources(auditIt.virtualFile, project)
        )
        assertFalse(
            "Precondition: AuditConsumer must live in production sources",
            TestSourcesFilter.isTestSources(consumer.virtualFile, project)
        )

        val production = injectionPoint(consumer, marker, PORTAL_APPLICATION)
        assertEquals("Precondition: a production query answers, got $production", "OK", production["status"].asText())
        assertEquals("Precondition: the injection mode answers, got $production", "INJECTION", production["mode"].asText())
        assertEquals(
            "Precondition: production code outside the test context finds no Audit, got $production",
            "NO_CANDIDATE",
            production["outcome"].asText()
        )

        val test = injectionPoint(auditIt, marker, LOCAL_APP)

        assertEquals("The test-source query answers, got $test", "OK", test["status"].asText())
        assertEquals(
            "A @SpringBootTest of the test application injects a component its scan reaches, got $test",
            "RESOLVED",
            test["outcome"].asText()
        )
        assertEquals("audit", test["candidates"][0]["name"].asText())
        assertEquals(AUDIT, test["candidates"][0]["type"].asText())
    }

    private suspend fun injectionPoint(file: PsiFile, marker: String, application: String): JsonNode {
        val filePath = McpSourceLocation.of(file.virtualFile, project).filePath
        assertNotNull("Precondition: the file has a project path", filePath)
        return mapper.readTree(
            toolset().findSpringBean(
                projectPath = project.basePath,
                applicationClassName = application,
                source = "STATIC",
                filePath = filePath,
                line = lineOf(file, marker),
            )
        )
    }

    private fun lineOf(file: PsiFile, marker: String): Int {
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        return document.getLineNumber(offset) + 1
    }

    private fun toolset(): SpringBeanMcpToolset =
        McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>().single()

    private companion object {
        const val PORTAL_APPLICATION = "app.portal.PortalApplication"
        const val LOCAL_APP = "app.LocalApp"
        const val AUDIT = "app.audit.Audit"

        @Language("java")
        val JAVA_PORTAL_APPLICATION = """
            package app.portal;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class PortalApplication {}
            """.trimIndent()

        @Language("java")
        val JAVA_AUDIT = """
            package app.audit;

            import org.springframework.stereotype.Component;

            @Component
            public class Audit {}
            """.trimIndent()

        @Language("java")
        val JAVA_LOCAL_APP = """
            package app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class LocalApp {}
            """.trimIndent()

        @Language("java")
        val JAVA_AUDIT_IT = """
            package app;

            import app.audit.Audit;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.boot.test.context.SpringBootTest;

            @SpringBootTest(classes = LocalApp.class)
            class AuditIT {
                @Autowired
                Audit audit;
            }
            """.trimIndent()

        @Language("java")
        val JAVA_AUDIT_CONSUMER = """
            package app.portal;

            import app.audit.Audit;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.stereotype.Component;

            @Component
            public class AuditConsumer {
                @Autowired
                Audit audit;
            }
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_PORTAL_APPLICATION = """
            package app.portal

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class PortalApplication
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_AUDIT = """
            package app.audit

            import org.springframework.stereotype.Component

            @Component
            class Audit
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_LOCAL_APP = """
            package app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class LocalApp
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_AUDIT_IT = """
            package app

            import app.audit.Audit
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.boot.test.context.SpringBootTest

            @SpringBootTest(classes = [LocalApp::class])
            class AuditIT {
                @Autowired
                lateinit var audit: Audit
            }
            """.trimIndent()

        @Language("kotlin")
        val KOTLIN_AUDIT_CONSUMER = """
            package app.portal

            import app.audit.Audit
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.stereotype.Component

            @Component
            class AuditConsumer {
                @Autowired
                lateinit var audit: Audit
            }
            """.trimIndent()
    }
}
