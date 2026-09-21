/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.editor.Document
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Which line of a method identifies it to `explyt_trace_spring_call_chain`.
 *
 * Every other Explyt tool hands out the **declaration** line — `explyt_find_spring_endpoint` reports a handler at
 * its signature — and starting a trace from it used to answer `no method found`, so the two tools disagreed about
 * the same Spring method and the caller had no way to learn which convention was expected.
 *
 * Uses the heavy [JavaCodeInsightFixtureTestCase] because `traceCallChain` resolves its file through
 * `LocalFileSystem.findFileByPath("${'$'}basePath/${'$'}filePath")`, which cannot see the in-memory `temp://` VFS of a
 * light fixture.
 */
class SpringBootApplicationMcpToolsetTraceLineTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testEveryLineOfAMethodStartsTheSameTrace() = runBlocking<Unit> {
        val source = """
            package com.example.app

            annotation class GetMapping(val value: String)

            class ActivityService {
                fun activity(id: Long): String = id.toString()
            }

            class ShortLinkAdminController(private val service: ActivityService) {
                @GetMapping("/activity")
                fun activity(id: Long): String {
                    return service.activity(id)
                }
            }
        """.trimIndent()
        val relativePath = addSource("declarationSrc", "com/example/app/AnnotatedController.kt", source)
        val document = documentOf(relativePath)

        val annotationLine = lineOf(document, source, "@GetMapping")
        val declarationLine = lineOf(document, source, "fun activity(id: Long): String {")
        val bodyLine = lineOf(document, source, "return service.activity(id)")
        assertEquals(
            "The three coordinates must differ for this test to distinguish them",
            3, setOf(annotationLine, declarationLine, bodyLine).size
        )

        for (line in listOf(annotationLine, declarationLine, bodyLine)) {
            val head = traceFrom(relativePath, line)["chain"][0]

            assertEquals("Tracing from line $line resolved the wrong method", "activity", head["methodName"].asText())
            assertTrue(
                "Tracing from line $line lost the ActivityService call",
                head["callsInto"].map { it["target"].asText() }.any { it.endsWith("ActivityService.activity") }
            )
        }
    }

    /**
     * A line belonging to no method has to stay a miss: resolving it by widening the search to the enclosing class
     * would silently trace a neighbouring method as if it were the one asked for, which is worse than refusing.
     * The refusal names the methods around it, because the caller cannot otherwise tell a wrong line from a file
     * this tool cannot read.
     */
    fun testLineOutsideAnyMethodIsRefusedWithTheNearestDeclarations() = runBlocking<Unit> {
        val source = """
            package com.example.app

            class GapController {
                fun first(): String = "first"

                fun second(): String = "second"
            }
        """.trimIndent()
        val relativePath = addSource("gapSrc", "com/example/app/GapController.kt", source)
        val document = documentOf(relativePath)
        val blankLine = lineOf(document, source, "fun first") + 1

        val failure = runCatching { traceFrom(relativePath, blankLine) }.exceptionOrNull()

        assertNotNull("A blank line between two methods must not resolve to either of them", failure)
        val message = failure!!.message.orEmpty()
        assertTrue("The refusal must name the methods around the line, got: $message", message.contains("first"))
        assertTrue("The refusal must name the methods around the line, got: $message", message.contains("second"))
    }

    private suspend fun traceFrom(relativePath: String, line: Int) = mapper.readTree(
        toolset.traceCallChain(
            filePath = relativePath,
            line = line,
            projectPath = project.basePath!!,
            depth = 2,
            includeTests = false,
        )
    )

    private fun lineOf(document: Document, source: String, anchor: String): Int {
        val offset = source.indexOf(anchor)
        assertTrue("Anchor '$anchor' is absent from the fixture", offset >= 0)
        return document.getLineNumber(offset) + 1
    }

    private fun documentOf(relativePath: String): Document {
        val file = LocalFileSystem.getInstance().findFileByPath("${project.basePath}/$relativePath")
            ?: error("Source not registered in VFS: $relativePath")
        val psiFile = PsiManager.getInstance(project).findFile(file) ?: error("PSI not available: $relativePath")
        return PsiDocumentManager.getInstance(project).getDocument(psiFile)!!
    }

    private fun addSource(rootName: String, relativePath: String, content: String): String {
        val sourcesRoot = File(project.basePath!!, rootName).apply { mkdirs() }
        File(sourcesRoot, relativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, true)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        return "$rootName/$relativePath"
    }
}
