/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInspection.LocalInspectionEP
import com.intellij.codeInspection.LocalInspectionTool
import com.intellij.openapi.util.registry.Registry
import org.intellij.lang.annotations.Language

class SpringConfigurationKeySpellcheckingTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(*spellcheckingInspections())
    }

    fun testDeclaredYamlKeyIsNotATypo() {
        assertEquals(setOf("resourcesrever"), typosIn("application.yaml", YAML_KEYS))
    }

    fun testYamlKeyOutsideSpringConfigurationIsStillChecked() {
        assertTrue(typosIn("pipeline.yaml", YAML_KEYS).containsAll(setOf("resourceserver", "resourcesrever")))
    }

    fun testDeclaredPropertiesKeyIsNotATypo() {
        assertEquals(setOf("resourcesrever"), typosIn("application.properties", PROPERTIES_KEYS))
    }

    fun testPropertiesKeyOutsideSpringConfigurationIsStillChecked() {
        assertTrue(typosIn("messages.properties", PROPERTIES_KEYS).containsAll(setOf("resourceserver", "resourcesrever")))
    }

    fun testListElementMemberBoundByTheBinderIsNotATypo() {
        @Language("kotlin") val properties = """
            import org.springframework.boot.context.properties.ConfigurationProperties
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConfigurationProperties(prefix = "explyt.ingest")
            class IngestProperties {
                var sources: List<Source> = emptyList()

                class Source {
                    var keystorepath: String = ""
                }
            }
        """.trimIndent()
        myFixture.addFileToProject("IngestProperties.kt", properties)

        val typos = typosIn(
            "application.yaml",
            """
            explyt:
              ingest:
                sources:
                  - keystorepath: true
                    truststorepth: true
            """.trimIndent()
        )

        assertEquals(setOf("truststorepth"), typos)
    }

    private fun typosIn(fileName: String, text: String): Set<String> {
        val typosPerMode = listOf(true, false).associateWith { textLevel ->
            Registry.get(TEXT_LEVEL_SPELLCHECKING).setValue(textLevel, testRootDisposable)
            myFixture.configureByText(fileName, text)
            myFixture.doHighlighting()
                .filter { it.severity.name == "TYPO" }
                .mapTo(mutableSetOf()) { it.text }
        }
        assertEquals("text-level and token-level spellchecking disagree", typosPerMode[true], typosPerMode[false])
        return typosPerMode.getValue(true)
    }

    private fun spellcheckingInspections(): Array<LocalInspectionTool> =
        listOf("SpellCheckingInspection", "GrazieInspectionRunner").map { shortName ->
            LocalInspectionEP.LOCAL_INSPECTION.extensionList
                .single { it.shortName == shortName }
                .instantiateTool() as LocalInspectionTool
        }.toTypedArray()

    private companion object {
        const val TEXT_LEVEL_SPELLCHECKING = "spellchecker.grazie.enabled"

        val YAML_KEYS = """
            spring:
              security:
                oauth2:
                  resourceserver:
                    jwt:
                      issuer-uri: true
                  resourcesrever:
                    enabled: true
        """.trimIndent()

        val PROPERTIES_KEYS = """
            spring.security.oauth2.resourceserver.jwt.issuer-uri=true
            spring.security.oauth2.resourcesrever.enabled=true
        """.trimIndent()
    }
}
