/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.util

import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.util.runReadNonBlocking
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

class IsConfigurationPropertyFileTest : ExplytBaseLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testApplicationPropertiesIsRecognized() {
        addConfigClass(propertySourcePath = null)
        val psiFile = myFixture.addFileToProject("application.properties", "server.port=8080")

        assertTrue(isConfigurationPropertyFile(psiFile))
    }

    fun testApplicationYamlIsRecognized() {
        addConfigClass(propertySourcePath = null)
        val psiFile = myFixture.addFileToProject("application.yaml", "server:\n  port: 8080")

        assertTrue(isConfigurationPropertyFile(psiFile))
    }

    fun testUnrelatedPropertiesIsNotRecognized() {
        addConfigClass(propertySourcePath = null)
        val psiFile = myFixture.addFileToProject("messages.properties", "greeting.hello=Hello")

        assertFalse(isConfigurationPropertyFile(psiFile))
    }

    fun testUnrelatedYamlIsNotRecognized() {
        addConfigClass(propertySourcePath = null)
        val psiFile = myFixture.addFileToProject("messages.yaml", "greeting:\n  hello: Hello")

        assertFalse(isConfigurationPropertyFile(psiFile))
    }

    fun testPropertySourcePropertiesIsRecognized() {
        addConfigClass(propertySourcePath = "classpath:custom.properties")
        val psiFile = myFixture.addFileToProject("custom.properties", "custom.key=value")

        assertTrue(isConfigurationPropertyFile(psiFile))
    }

    fun testPropertySourceYamlIsRecognized() {
        addConfigClass(propertySourcePath = "classpath:custom.yml")
        val psiFile = myFixture.addFileToProject("custom.yml", "custom:\n  key: value")

        assertTrue(isConfigurationPropertyFile(psiFile))
    }

    fun testBecomesPropertySourceAfterEdit() {
        val configFile = addConfigClass(propertySourcePath = null)
        val psiFile = myFixture.addFileToProject("custom.properties", "custom.key=value")

        assertFalse("not a property source before the edit", isConfigurationPropertyFile(psiFile))

        setFileText(configFile, configClassText("classpath:custom.properties"))

        assertTrue("must be a property source after the edit", isConfigurationPropertyFile(psiFile))
    }

    fun testStopsBeingPropertySourceAfterEdit() {
        val configFile = addConfigClass(propertySourcePath = "classpath:custom.properties")
        val psiFile = myFixture.addFileToProject("custom.properties", "custom.key=value")

        assertTrue("a property source before the edit", isConfigurationPropertyFile(psiFile))

        setFileText(configFile, configClassText(null))

        assertFalse("must not be a property source after the edit", isConfigurationPropertyFile(psiFile))
    }

    fun testBecomesYamlPropertySourceAfterEdit() {
        val configFile = addConfigClass(propertySourcePath = null)
        val psiFile = myFixture.addFileToProject("custom.yml", "custom:\n  key: value")

        assertFalse("not a property source before the edit", isConfigurationPropertyFile(psiFile))

        setFileText(configFile, configClassText("classpath:custom.yml"))

        assertTrue("must be a property source after the edit", isConfigurationPropertyFile(psiFile))
    }

    private fun addConfigClass(propertySourcePath: String?): PsiFile =
        myFixture.addFileToProject("com/example/AppConfig.java", configClassText(propertySourcePath))

    private fun configClassText(propertySourcePath: String?): String {
        val annotation = propertySourcePath?.let { "@PropertySource(\"$it\")\n" } ?: ""
        return """
            package com.example;
            
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.PropertySource;
            
            @Configuration
            $annotation public class AppConfig {
            }
        """.trimIndent()
    }

    private fun setFileText(psiFile: PsiFile, text: String) {
        val documentManager = PsiDocumentManager.getInstance(project)
        WriteCommandAction.runWriteCommandAction(project) {
            val document = documentManager.getDocument(psiFile) ?: error("no document for ${psiFile.name}")
            document.replaceString(0, document.textLength, text)
            documentManager.commitDocument(document)
        }
    }

    private fun isConfigurationPropertyFile(psiFile: PsiFile): Boolean =
        ApplicationManager.getApplication()
            .executeOnPooledThread<Boolean> { runReadNonBlocking { SpringCoreUtil.isConfigurationPropertyFile(psiFile) } }
            .get()
}
