/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.UnrelatedPsiChange.NOTE_EDITED
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/**
 * A client walks a large answer of the real tools page by page with the first page's revision. The walk must reach
 * the last page across an edit that leaves the Spring model as it was, and must be refused once the model changes.
 */
class PagedAnswerWalkTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private lateinit var notes: PsiFile
    private lateinit var walkController: PsiFile

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("springBootApp", "")
        walkController = myFixture.addFileToProject(WALK_CONTROLLER, walkControllerSource(ROUTES))
        notes = myFixture.addFileToProject("notes.md", "# Notes\n")
    }

    fun testAWalkOverEveryPageServesEveryRouteOnceInOrder() = runBlockingReleasingWriteIntent<Unit> {
        val pages = walk()

        assertEquals(listOf(PAGE_SIZE, PAGE_SIZE, PAGE_SIZE), pages.map { it["endpoints"].size() })
        assertEquals(expectedPaths(ROUTES), pages.flatMap(::paths))
        assertEquals(1, pages.map { it["revision"].asText() }.distinct().size)
        assertTrue("The last page ends the walk, got ${pages.last()}", pages.last()["nextOffset"].isNull)
    }

    fun testAWalkProjectsTheAnswerOnceForAllItsPages() = runBlockingReleasingWriteIntent<Unit> {
        val first = listingPage(offset = 0, revision = null)
        assertTruncatedFirstPage(LIST, first)
        val projected = cachedListingAnswer()
        assertEquals(ROUTES, projected.totalCount)

        val second = listingPage(first["nextOffset"].asInt(), first["revision"].asText())
        assertContinued(LIST, "nothing", second)
        val third = listingPage(second["nextOffset"].asInt(), first["revision"].asText())
        assertContinued(LIST, "nothing", third)

        assertSame("Pages 2 and 3 must be served from page 1's projection", projected, cachedListingAnswer())
    }

    fun testAWalkContinuesAcrossANoteEditedBetweenEveryPage() = runBlockingReleasingWriteIntent<Unit> {
        val pages = walk { NOTE_EDITED.applyTo(project) { notes } }

        assertEquals(3, pages.size)
        assertEquals(expectedPaths(ROUTES), pages.flatMap(::paths))
    }

    fun testAWalkIsRefusedOnceAMappingIsAddedBetweenPages() = runBlockingReleasingWriteIntent<Unit> {
        val first = listingPage(offset = 0, revision = null)
        assertTruncatedFirstPage(LIST, first)
        val second = listingPage(first["nextOffset"].asInt(), first["revision"].asText())
        assertContinued(LIST, "nothing", second)

        insertBeforeLastBrace(walkController, "@GetMapping(\"/added\") public String added() { return \"added\"; }")

        assertResultChanged(LIST, "a new @GetMapping", listingPage(second["nextOffset"].asInt(), first["revision"].asText()))
    }

    fun testAMissNeighbourhoodCutOnPage1IsServedWholeUnderPage1Revision() = runBlockingReleasingWriteIntent<Unit> {
        val (first, whole) = missNeighbourhoodPages(between = {})

        assertContinued(FIND, "nothing", whole)
        assertNeighbourhoodContinued(first, whole)
    }

    fun testAMissNeighbourhoodContinuesAcrossANoteEdit() = runBlockingReleasingWriteIntent<Unit> {
        val (first, whole) = missNeighbourhoodPages(between = { NOTE_EDITED.applyTo(project) { notes } })

        assertContinued(FIND, NOTE_EDITED.description, whole)
        assertNeighbourhoodContinued(first, whole)
    }

    fun testAMissPastItsEndContinuesWithPage1Revision() = runBlockingReleasingWriteIntent<Unit> {
        val first = findMiss(MISS_BUDGET)
        assertTrue("Precondition: the neighbourhood is cut, got $first", first[NEAREST_TRUNCATED]?.asBoolean() == true)

        val past = findMiss(MISS_BUDGET, offset = 1, revision = first["revision"].asText())

        assertContinued(FIND, "nothing", past)
        assertEquals("A neighbourhood is served on the first page only, got $past", 0, past["nearestByPrefix"].size())
    }

    private suspend fun missNeighbourhoodPages(between: () -> Unit): Pair<JsonNode, JsonNode> {
        val first = findMiss(MISS_BUDGET)
        assertEquals("Precondition: the URL is a miss, got $first", 0, first["totalCount"].asInt())
        assertTrue("Precondition: the neighbourhood is cut, got $first", first[NEAREST_TRUNCATED]?.asBoolean() == true)
        assertTrue("Precondition: page 1 serves part of the neighbourhood, got $first", first["nearestByPrefix"].size() > 0)

        between()

        return first to findMiss(BoundedPageWriter.MAX_CHARS, revision = first["revision"].asText())
    }

    private fun assertNeighbourhoodContinued(first: JsonNode, whole: JsonNode) {
        val served = paths(first, "nearestByPrefix")
        val all = paths(whole, "nearestByPrefix")
        assertFalse("The whole neighbourhood is not cut, got $whole", whole.has(NEAREST_TRUNCATED))
        assertTrue("The whole neighbourhood holds more than page 1, got $all", all.size > served.size)
        assertEquals("Page 1 served the nearest routes first", all.take(served.size), served)
        assertEquals(first["revision"].asText(), whole["revision"].asText())
    }

    private suspend fun walk(betweenPages: () -> Unit = {}): List<JsonNode> {
        val pages = mutableListOf(listingPage(offset = 0, revision = null))
        assertTruncatedFirstPage(LIST, pages.first())
        val revision = pages.first()["revision"].asText()
        while (!pages.last()["nextOffset"].isNull) {
            assertTrue("The walk did not terminate", pages.size < MAX_PAGES)
            betweenPages()
            val next = listingPage(pages.last()["nextOffset"].asInt(), revision)
            assertContinued(LIST, "page ${pages.size}", next)
            pages.add(next)
        }
        return pages
    }

    private suspend fun listingPage(offset: Int, revision: String?): JsonNode = mapper.readTree(
        toolset.getHttpEndpoints(
            projectPath = project.basePath,
            controllerFilter = WALK_CONTROLLER_NAME,
            compact = true,
            limit = PAGE_SIZE,
            offset = offset,
            expectedRevision = revision,
        )
    )

    private suspend fun findMiss(maxChars: Int, offset: Int = 0, revision: String? = null): JsonNode = mapper.readTree(
        toolset.findEndpoint(
            urlPattern = MISS_URL,
            projectPath = project.basePath,
            maxChars = maxChars,
            offset = offset,
            expectedRevision = revision,
        )
    )

    private fun cachedListingAnswer(): WholeAnswer = WholeAnswerCache.getInstance(project).answerFor(
        mapOf(
            "tool" to LIST,
            "controllerFilter" to WALK_CONTROLLER_NAME,
            "endpointType" to null,
            "compact" to "true",
            "beanFilterEnabled" to SpringToolRunConfigurationsSettingsState.getInstance().isBeanFilterEnabled.toString(),
        )
    ) { error("The listing answer must already be projected for this PSI state") }

    private fun insertBeforeLastBrace(file: PsiFile, member: String) {
        val documents = PsiDocumentManager.getInstance(project)
        val document = documents.getDocument(file) ?: error("${file.name} has no document")
        WriteCommandAction.runWriteCommandAction(project) {
            document.insertString(document.text.lastIndexOf('}'), "    $member\n")
            documents.commitDocument(document)
        }
    }

    private fun paths(page: JsonNode, field: String = "endpoints"): List<String> = page[field].map { it["fullPath"].asText() }

    private fun expectedPaths(routes: Int): List<String> = (0 until routes).map { "/api/walk/${routeName(it)}" }

    private companion object {
        const val LIST = "explyt_get_spring_http_endpoints"
        const val FIND = "explyt_find_spring_endpoint"
        const val ROUTES = 60
        const val PAGE_SIZE = 20
        const val MAX_PAGES = 10
        const val MISS_URL = "/api/unknown"
        const val MISS_BUDGET = 700
        const val NEAREST_TRUNCATED = "nearestByPrefixTruncated"
        const val WALK_CONTROLLER_NAME = "WalkController"
        const val WALK_CONTROLLER = "com/example/app/web/WalkController.java"

        fun routeName(index: Int) = "r" + index.toString().padStart(2, '0')

        fun walkControllerSource(routes: Int): String = buildString {
            appendLine("package com.example.app.web;")
            appendLine()
            appendLine("import org.springframework.web.bind.annotation.GetMapping;")
            appendLine("import org.springframework.web.bind.annotation.RequestMapping;")
            appendLine("import org.springframework.web.bind.annotation.RestController;")
            appendLine()
            appendLine("@RestController")
            appendLine("@RequestMapping(\"/api/walk\")")
            appendLine("public class WalkController {")
            (0 until routes).map(::routeName).forEach { name ->
                appendLine("    @GetMapping(\"/$name\") public String $name() { return \"$name\"; }")
            }
            appendLine("}")
        }
    }
}
