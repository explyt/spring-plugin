/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.ai.mcp

import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
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
 * Heavy-fixture test for `explyt_trace_spring_call_chain`.
 *
 * Uses [JavaCodeInsightFixtureTestCase] instead of the light fixture sibling test because
 * `traceCallChain` resolves files via `LocalFileSystem.findFileByPath("$basePath/$filePath")`,
 * which cannot see files in the in-memory `temp://` VFS used by light fixtures. Sources here
 * live on real disk under `project.basePath`.
 *
 * The fixture deliberately uses **qualified** method calls (`this.findById(id)` and
 * `service.findById(id)`) because those are the real-world pattern in Spring code, and the
 * previous implementation of `findCalledMethods` silently dropped them.
 */
class SpringBootApplicationMcpToolsetTraceCallChainTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    fun testTraceCallChainHappyPath() = runBlocking<Unit> {
        val basePath = project.basePath!!
        val sourcesRoot = File(basePath, "src").apply { mkdirs() }

        // Controller -> Service -> Repository, single compilation unit so no cross-file
        // resolution is required. All calls are qualified, which is the realistic Spring style.
        writeJava(
            sourcesRoot, "com/example/app/App.java",
            """
            package com.example.app;

            public class App {
                public static class DemoRepository {
                    public String load(Long id) { return "item-" + id; }
                }
                public static class DemoService {
                    private final DemoRepository repository;
                    public DemoService(DemoRepository repository) { this.repository = repository; }
                    public String findById(Long id) { return this.repository.load(id); }
                }
                public static class DemoController {
                    private final DemoService service;
                    public DemoController(DemoService service) { this.service = service; }
                    public String getItem(Long id) {
                        return service.findById(id);
                    }
                }
            }
            """.trimIndent()
        )

        WriteAction.runAndWait<Throwable> {
            LocalFileSystem.getInstance().refresh(false)
        }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, true)
        }
        WriteAction.runAndWait<Throwable> {
            LocalFileSystem.getInstance().refresh(false)
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)

        val appVf = LocalFileSystem.getInstance()
            .findFileByPath("$basePath/src/com/example/app/App.java")
            ?: error("App file not registered in VFS")
        val appPsi = PsiManager.getInstance(project).findFile(appVf)
            ?: error("App PSI not available")
        val relativePath = "src/com/example/app/App.java"

        // Point at a line inside the `getItem` body so that whitespace at line start still has
        // the containing method as a UAST parent.
        val anchorOffset = appPsi.text.indexOf("return service.findById(id);")
        assertTrue("Expected anchor text to exist in App.java", anchorOffset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(appPsi)!!
        val getItemLine = document.getLineNumber(anchorOffset) + 1

        val resultJson = toolset.traceCallChain(
            filePath = relativePath,
            line = getItemLine,
            projectPath = basePath,
            depth = 3,
            includeTests = false,
        )
        val result = mapper.readTree(resultJson)

        val chain = result["chain"]
        assertNotNull("Expected 'chain' field in $result", chain)
        assertTrue("Expected non-empty chain in $result", chain.size() > 0)

        // Head node: the method we pointed at.
        val head = chain[0]
        assertEquals("getItem", head["methodName"].asText())
        assertTrue(
            "Expected head class to be DemoController, got ${head["className"]}",
            head["className"].asText().endsWith("DemoController")
        )
        assertEquals(
            "Expected file path to match the one passed into traceCallChain",
            relativePath, head["filePath"].asText()
        )
        assertTrue(
            "Expected head line to be a positive line of the method declaration, got ${head["line"]}",
            head["line"].asInt() in 1..getItemLine
        )

        // Head must record its direct call target (DemoService.findById) — this covers the
        // qualified-call resolution fix in `findCalledMethods`.
        val headCallsInto = head["callsInto"].map { it["target"].asText() }
        assertTrue(
            "Expected getItem to call into DemoService.findById, got $headCallsInto",
            headCallsInto.any { it.endsWith("DemoService.findById") }
        )

        // Chain must traverse all three layers.
        val methods = chain.map { it["methodName"].asText() }.toSet()
        assertTrue("Expected 'findById' in the chain, got $methods", methods.contains("findById"))
        assertTrue("Expected 'load' in the chain, got $methods", methods.contains("load"))

        // Every chain node must point back at the same relative file path.
        val filePaths = chain.mapNotNull { it["filePath"]?.asText() }.toSet()
        assertEquals(
            "Expected all chain entries to reference $relativePath, got $filePaths",
            setOf(relativePath), filePaths
        )

        // includeTests=false ⇒ testReferences must be empty.
        val testRefs = result["testReferences"]
        assertNotNull("Expected 'testReferences' field in $result", testRefs)
        assertEquals(
            "Expected empty testReferences when includeTests=false, got $testRefs",
            0, testRefs.size()
        )
    }

    private fun writeJava(sourcesRoot: File, relativePath: String, content: String) {
        val target = File(sourcesRoot, relativePath)
        target.parentFile.mkdirs()
        target.writeText(content)
    }
}
