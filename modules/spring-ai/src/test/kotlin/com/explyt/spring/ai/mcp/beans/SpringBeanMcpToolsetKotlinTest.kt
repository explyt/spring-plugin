/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

/**
 * The question the tool exists for, asked the way a client asks it.
 *
 * A Kotlin parameter carrying a default needs two independent answers - is there a bean, and does the parameter
 * have to be satisfied at all - and the pair only means something when both travel in one response.
 */
class SpringBeanMcpToolsetKotlinTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.kotlin_1_9_22,
        TestLibrary("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    )

    private val mapper = ObjectMapper()

    /** One bean exists, so Spring injects it and the default is never evaluated. */
    fun testDefaultedKotlinParameterWithOneBeanResolves() = runBlocking {
        val root = injectionAnswer()

        assertEquals("RESOLVED", root["outcome"].asText())
        assertEquals("utcClock", root["candidates"][0]["name"].asText())
    }

    /**
     * The facts that answer "will this start?" - independent of whether a candidate was found.
     *
     * A defaulted parameter is not required, and reporting it as required would turn a working constructor into
     * a reported failure.
     */
    fun testTheDefaultIsReportedBesideTheCandidate() = runBlocking {
        val injection = injectionAnswer()["injection"]

        assertEquals("clock", injection["name"].asText())
        assertEquals("SINGLE", injection["shape"].asText())
        assertEquals(false, injection["required"].asBoolean())
        assertEquals(true, injection["hasDefaultValue"].asBoolean())
        assertTrue("basis must name the evidence", injection["basis"].asText().isNotBlank())
    }

    /** The whole answer still has to fit what a client will accept. */
    fun testTheKotlinAnswerFitsTheClientBudget() = runBlocking {
        val json = call()

        assertEquals("OK", mapper.readTree(json)["status"].asText())
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
        assertTrue("wrapped payload was ${wrappedLength(json)} chars", wrappedLength(json) <= MAX_CLIENT_PAYLOAD)
    }

    private suspend fun injectionAnswer(): JsonNode = mapper.readTree(call())

    private suspend fun call(): String {
        val file = copyFixture()
        val (line, column) = defaultedParameterPosition(file)
        return toolset().findSpringBean(
            projectPath = project.basePath!!,
            applicationClassName = "com.explyt.demo.KotlinApp",
            source = "STATIC",
            filePath = "com/explyt/demo/DefaultClock.kt",
            line = line,
            column = column
        )
    }

    private fun toolset(): SpringBeanMcpToolset {
        val toolsets = McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>()
        assertEquals("Precondition: the toolset must be registered", 1, toolsets.size)
        return toolsets.single()
    }

    private fun copyFixture(): PsiFile {
        myFixture.copyDirectoryToProject("beanQuery", "")
        val file = myFixture.findFileInTempDir("com/explyt/demo/DefaultClock.kt")
        val psiFile = myFixture.psiManager.findFile(file)
        assertNotNull("Precondition: the fixture must load, otherwise nothing is proven", psiFile)
        return psiFile!!
    }

    /** The defaulted `clock` parameter, located by its text so the fixture can be edited without breaking this. */
    private fun defaultedParameterPosition(file: PsiFile): Pair<Int, Int> {
        val marker = "clock: Clock = Clock.systemUTC()"
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val line = document.getLineNumber(offset) + 1
        return line to (offset - document.getLineStartOffset(line - 1) + 1)
    }

    private fun wrappedLength(json: String): Int =
        mapper.writeValueAsString(mapper.createArrayNode().add(json)).length

    private companion object {
        const val MAX_DEFAULT_PAYLOAD = 1800
        const val MAX_CLIENT_PAYLOAD = 2000
    }
}
