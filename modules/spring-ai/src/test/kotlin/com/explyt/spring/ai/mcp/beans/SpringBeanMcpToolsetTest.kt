/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanApplicationResolver
import com.explyt.spring.core.service.beans.SpringInjectionPointResolver
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import kotlinx.coroutines.runBlocking

/**
 * The bean lookup tool as a client reaches it: through the registered toolset, over a real fixture.
 *
 * Reflection over the class would pass while the tool is invisible to every client, so the extension point is
 * what these tests resolve the toolset from.
 */
class SpringBeanMcpToolsetTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
    )

    private val mapper = ObjectMapper()

    fun testToolsetIsRegisteredForClients() {
        val toolsets = McpToolset.EP.extensionList
        assertEquals(
            "The bean lookup toolset must be registered in mcp-server-plugin.xml",
            1,
            toolsets.filterIsInstance<SpringBeanMcpToolset>().size
        )
    }

    /** The tool's own purpose, end to end: a bean asked for by one of its names, found through the extension. */
    fun testLookupClockThroughRegisteredToolset() = runBlocking {
        copyBeanQueryFixture()

        val root = call(beanName = "utcClock")

        assertEquals("OK", root["status"].asText())
        assertEquals("LOOKUP", root["mode"].asText())
        assertEquals("SINGLE", root["outcome"].asText())
        assertEquals("COMPLETE", root["matchCompleteness"].asText())
        assertEquals(1, root["totalCount"].asInt())
        val candidate = root["candidates"][0]
        assertEquals("systemClock", candidate["name"].asText())
        assertEquals("utcClock", candidate["matchedName"].asText())
        assertEquals("java.time.Clock", candidate["type"].asText())
        assertTrue(declarationOf(candidate).endsWith("TimeConfig.java"))
    }

    /** A bean declared with an SDK return type must not fall out because its type lives outside the project. */
    fun testLookupByTypeFindsALibraryTypedBean() = runBlocking {
        copyBeanQueryFixture()

        val root = call(typeFqn = "java.time.Clock")

        assertEquals("SINGLE", root["outcome"].asText())
        assertEquals("systemClock", root["candidates"][0]["name"].asText())
    }

    /** The static model is named as an estimate rather than presented as the running context. */
    fun testStaticAnswerNamesItsPrecision() = runBlocking {
        copyBeanQueryFixture()

        val model = call(beanName = "systemClock")["model"]

        assertEquals("STATIC", model["source"].asText())
        assertEquals("MODULE_ESTIMATE", model["precision"].asText())
        assertEquals("com.explyt.demo.App", model["application"].asText())
    }

    fun testUnknownNameIsNoneWithinTheKnownNames() = runBlocking {
        copyBeanQueryFixture()

        val root = call(beanName = "nowhereClock")

        assertEquals("NONE", root["outcome"].asText())
        assertEquals(0, root["totalCount"].asInt())
        assertFalse(root["truncated"].asBoolean())
        assertTrue(root["nextOffset"].isNull)
    }

    /** An injection point answers about the constructor parameter, with the declaration facts beside it. */
    fun testInjectionPointResolvesTheDeclaredParameter() = runBlocking {
        val file = copyBeanQueryFixture()
        val (line, column) = positionOf(file, "ClockConsumer(Clock clock)", "ClockConsumer(Clock ".length)

        val root = call(filePath = "com/explyt/demo/ClockConsumer.java", line = line, column = column)

        assertEquals("INJECTION", root["mode"].asText())
        assertEquals("RESOLVED", root["outcome"].asText())
        val injection = root["injection"]
        assertEquals("clock", injection["name"].asText())
        assertEquals("SINGLE", injection["shape"].asText())
        assertTrue(injection["required"].asBoolean())
        assertFalse(injection["hasDefaultValue"].asBoolean())
        assertEquals("systemClock", root["candidates"][0]["name"].asText())
        assertTrue(declarationOf(root["candidates"][0]).endsWith("TimeConfig.java"))
    }

    /**
     * A plain field is not an injection point, and saying so differs from saying no bean exists.
     *
     * The field holds the very type a bean is declared for, so a resolver that answered about it anyway would
     * return a confident RESOLVED - the mistake this distinction exists to prevent.
     */
    fun testAnOrdinaryFieldIsNotAnInjectionPoint() = runBlocking {
        val file = copyBeanQueryFixture()
        val (line, _) = positionOf(file, "private final Clock clock;")

        val root = call(filePath = "com/explyt/demo/ClockConsumer.java", line = line)

        assertEquals("ERROR", root["status"].asText())
        assertEquals(
            SpringInjectionPointResolver.UNSUPPORTED_INJECTION_POINT,
            root["error"]["code"].asText()
        )
    }

    /** A selection problem is an error a caller can act on, never an empty result that reads as "no beans". */
    fun testAnUnknownApplicationIsReportedAsAProblem() = runBlocking {
        copyBeanQueryFixture()

        val root = call(beanName = "systemClock", applicationClassName = "com.explyt.demo.Missing")

        assertEquals("ERROR", root["status"].asText())
        assertEquals(BeanApplicationResolver.APPLICATION_NOT_FOUND, root["error"]["code"].asText())
    }

    /** An invalid argument is refused before any model is read, and named as such. */
    fun testAMixedModeIsRefused() = runBlocking {
        copyBeanQueryFixture()

        val root = call(beanName = "systemClock", filePath = "com/explyt/demo/ClockConsumer.java", line = 12)

        assertEquals("ERROR", root["status"].asText())
        assertEquals(BoundedBeanResponseWriter.INVALID_ARGUMENT, root["error"]["code"].asText())
    }

    /**
     * A project path naming no open project is refused rather than answered from whichever project is open: a
     * confident answer about the wrong project cannot be told from a correct one.
     */
    fun testAnUnknownProjectPathIsNotAnsweredFromTheOpenOne() = runBlocking {
        copyBeanQueryFixture()

        val json = toolset().findSpringBean(projectPath = "/nowhere/at/all", beanName = "systemClock")
        val root = mapper.readTree(json)

        assertEquals("ERROR", root["status"].asText())
        assertEquals(BeanLookupService.PROJECT_NOT_FOUND, root["error"]["code"].asText())
    }

    /**
     * The default answer has to fit the budget a client applies to it, not merely be short.
     *
     * The answer is asserted to be a real one first: any empty or failed response would satisfy a size bound on
     * its own and prove nothing about the projection that has to fit inside it.
     */
    fun testTheDefaultAnswerFitsTheClientBudget() = runBlocking {
        copyBeanQueryFixture()

        val json = toolset().findSpringBean(projectPath = projectPath(), beanName = "systemClock")
        val root = mapper.readTree(json)

        assertEquals("Precondition: the budget of a failed answer proves nothing", "OK", root["status"].asText())
        assertEquals(1, root["totalCount"].asInt())
        assertTrue("payload was ${json.length} chars", json.length <= MAX_DEFAULT_PAYLOAD)
        assertTrue(
            "MCP-wrapped payload was ${wrappedLength(json)} chars",
            wrappedLength(json) <= MAX_CLIENT_PAYLOAD
        )
    }

    /** A continuation is bound to the question it continues, so a changed filter cannot reuse its offset. */
    fun testAContinuationOfADifferentQueryIsRefused() = runBlocking {
        copyBeanQueryFixture()

        val first = call(beanName = "systemClock")
        val root = call(beanName = "utcClock", offset = 1, expectedRevision = first["revision"].asText())

        assertEquals("ERROR", root["status"].asText())
        assertEquals(BoundedBeanResponseWriter.RESULT_CHANGED, root["error"]["code"].asText())
    }

    private suspend fun call(
        typeFqn: String? = null,
        beanName: String? = null,
        filePath: String? = null,
        line: Int? = null,
        column: Int? = null,
        applicationClassName: String? = "com.explyt.demo.App",
        offset: Int = 0,
        expectedRevision: String? = null
    ): JsonNode = mapper.readTree(
        toolset().findSpringBean(
            projectPath = projectPath(),
            applicationClassName = applicationClassName,
            source = "STATIC",
            typeFqn = typeFqn,
            beanName = beanName,
            filePath = filePath,
            line = line,
            column = column,
            offset = offset,
            expectedRevision = expectedRevision
        )
    )

    private fun toolset(): SpringBeanMcpToolset {
        val toolsets = McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>()
        assertEquals("Precondition: the toolset must be registered", 1, toolsets.size)
        return toolsets.single()
    }

    private fun copyBeanQueryFixture(): PsiFile {
        myFixture.copyDirectoryToProject("beanQuery", "")
        val file = myFixture.findFileInTempDir("com/explyt/demo/ClockConsumer.java")
        val psiFile = myFixture.psiManager.findFile(file)
        assertNotNull("Precondition: the fixture must load, otherwise nothing is proven", psiFile)
        return psiFile!!
    }

    private fun positionOf(file: PsiFile, marker: String, offsetInMarker: Int = 0): Pair<Int, Int> {
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val target = offset + offsetInMarker
        val line = document.getLineNumber(target) + 1
        return line to (target - document.getLineStartOffset(line - 1) + 1)
    }

    private fun projectPath(): String = project.basePath!!

    /**
     * Where a candidate was declared, however the projection could express it.
     *
     * A file under the project root is reported as a relative `filePath`, and anything else - a library, or the
     * light fixture's own `temp://` source root, which sits outside the project's base directory - as a
     * `sourceUrl`. Both are a declaration; asserting only the first would make this test depend on the fixture's
     * storage rather than on the answer.
     */
    private fun declarationOf(candidate: JsonNode): String {
        val declaration = candidate["declaration"]
        assertNotNull("A bean declared in the fixture must carry a declaration", declaration)
        return (declaration["filePath"] ?: declaration["sourceUrl"]).asText()
    }

    /** The size a client measures: the payload inside the content array the MCP transport wraps it in. */
    private fun wrappedLength(json: String): Int =
        mapper.writeValueAsString(mapper.createArrayNode().add(json)).length

    private companion object {
        const val MAX_DEFAULT_PAYLOAD = 1800
        const val MAX_CLIENT_PAYLOAD = 2000
    }
}
