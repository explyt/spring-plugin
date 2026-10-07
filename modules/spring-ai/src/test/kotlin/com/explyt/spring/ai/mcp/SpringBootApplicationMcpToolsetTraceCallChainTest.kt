/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.addFromMaven
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.openapi.application.WriteAction
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.uast.UCallExpression
import org.jetbrains.uast.UMethod
import org.jetbrains.uast.visitor.AbstractUastVisitor
import org.jetbrains.uast.toUElement
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

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    fun testTraceCallChainHappyPath() = runBlocking<Unit> {
        val basePath = project.basePath!!
        val sourcesRoot = File(basePath, "src").apply { mkdirs() }

        // Controller -> Service -> Repository, single compilation unit so no cross-file
        // resolution is required. All calls are qualified, which is the realistic Spring style.
        writeSource(
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

        assertTrue(
            "No test reference may be reported when includeTests=false, got $chain",
            chain.none { it.has("testReferences") || it.has("testUrlReferences") }
        )
    }

    fun testTraceKotlinLightCallee() = runBlocking {
        val basePath = project.basePath!!
        val sourcesRoot = File(basePath, "kotlinSrc").apply { mkdirs() }
        val relativePath = "kotlinSrc/com/example/app/Controller.kt"
        val source = """
            package com.example.app

            class DemoService {
                fun findById(id: Long): String = id.toString()
            }

            class DemoController(private val service: DemoService) {
                fun getItem(id: Long): String {
                    return service.findById(id)
                }
            }
        """.trimIndent()
        writeSource(sourcesRoot, "com/example/app/Controller.kt", source)
        registerSourceRoot(sourcesRoot)

        val appVf = LocalFileSystem.getInstance().findFileByPath("$basePath/$relativePath")
            ?: error("Kotlin source not registered in VFS")
        val appPsi = PsiManager.getInstance(project).findFile(appVf) as? KtFile
            ?: error("Kotlin PSI not available")
        val controllerMethod = appPsi.declarations
            .filterIsInstance<KtClass>()
            .first { it.name == "DemoController" }
            .declarations
            .filterIsInstance<KtNamedFunction>()
            .single { it.name == "getItem" }
        val resolvedCallee = resolvedCalls(controllerMethod.toUElement() as UMethod)
            .single { it.name == "findById" }

        assertTrue(
            "Expected Kotlin UAST callee to be a light method, got ${resolvedCallee.javaClass.name}",
            resolvedCallee is KtLightMethod
        )
        assertNotNull("Expected Kotlin light method to have a source range in this fixture", resolvedCallee.textRange)
        assertNotNull("Expected light method to navigate to source PSI", resolvedCallee.navigationElement.textRange)

        val document = PsiDocumentManager.getInstance(project).getDocument(appPsi)!!
        val callLine = document.getLineNumber(source.indexOf("return service.findById(id)")) + 1
        val declarationLine = document.getLineNumber(source.indexOf("fun findById")) + 1
        val result = mapper.readTree(
            toolset.traceCallChain(
                filePath = relativePath,
                line = callLine,
                projectPath = basePath,
                depth = 2,
                includeTests = false,
            )
        )

        val head = result["chain"].first { it["methodName"].asText() == "getItem" }
        val callee = result["chain"].first { it["methodName"].asText() == "findById" }
        val call = head["callsInto"].single { it["target"].asText().endsWith("DemoService.findById") }
        assertEquals("A call is reported where it is made", callLine, call["line"].asInt())
        assertEquals("The call points at the node of the method it reaches", callee["id"].asInt(), call["node"].asInt())
        assertEquals(declarationLine, callee["line"].asInt())
    }

    fun testTraceGeneratedDataClassCalleeLine() = runBlocking {
        // Generated `copy()` of a Kotlin data class: a light callee whose reported line must stay inside the
        // declaring file rather than being fabricated. See the unit tests for the null-range fallback itself.
        val basePath = project.basePath!!
        val sourcesRoot = File(basePath, "syntheticSrc").apply { mkdirs() }
        val relativePath = "syntheticSrc/com/example/app/Synthetic.kt"
        val source = """
            package com.example.app

            data class Item(val id: Long, val title: String)

            class ItemController {
                fun rename(item: Item): Item {
                    return item.copy(title = "renamed")
                }
            }
        """.trimIndent()
        writeSource(sourcesRoot, "com/example/app/Synthetic.kt", source)
        registerSourceRoot(sourcesRoot)

        val appVf = LocalFileSystem.getInstance().findFileByPath("$basePath/$relativePath")
            ?: error("Kotlin source not registered in VFS")
        val appPsi = PsiManager.getInstance(project).findFile(appVf) as? KtFile
            ?: error("Kotlin PSI not available")
        val renameFunction = appPsi.declarations
            .filterIsInstance<KtClass>()
            .first { it.name == "ItemController" }
            .declarations
            .filterIsInstance<KtNamedFunction>()
            .single { it.name == "rename" }
        val syntheticCallee = resolvedCalls(renameFunction.toUElement() as UMethod)
            .single { it.name == "copy" }

        // In this fixture `copy()` resolves to a light method that still carries a range, so the chain must
        // report the generated member at its declaring `data class`.
        assertTrue("Expected copy() to resolve to a light method", syntheticCallee is KtLightMethod)

        val document = PsiDocumentManager.getInstance(project).getDocument(appPsi)!!
        val callLine = document.getLineNumber(source.indexOf("return item.copy(")) + 1
        val itemClassLine = document.getLineNumber(source.indexOf("data class Item")) + 1
        val result = mapper.readTree(
            toolset.traceCallChain(
                filePath = relativePath,
                line = callLine,
                projectPath = basePath,
                depth = 2,
                includeTests = false,
            )
        )

        val head = result["chain"].first { it["methodName"].asText() == "rename" }
        val copyTarget = head["callsInto"].single { it["target"].asText().endsWith("Item.copy") }
        assertEquals("A call is reported where it is made", callLine, copyTarget["line"].asInt())
        val copyNode = result["chain"].single { it["id"].asInt() == copyTarget["node"].asInt() }
        assertEquals(
            "Expected the generated copy() to be reported at its declaring data class",
            itemClassLine, copyNode["line"].asInt()
        )
    }

    fun testCompanionCallNamesItsOuterClass() = runBlocking {
        val chain = traceCompanion("kotlinParse")
        val call = companionCall(chain, "parse")
        assertEquals("PROJECT", call["kind"].asText())
        assertEquals("VmId.parse", call["target"].asText())
    }

    fun testJavaJvmStaticCallNamesItsOuterClass() = runBlocking {
        val call = companionCall(traceCompanion("javaStaticParse", javaCaller = true), "parse")
        assertEquals("PROJECT", call["kind"].asText())
        assertEquals("StaticVmId.parse", call["target"].asText())
    }

    fun testJavaCompanionCallNamesItsOuterClass() = runBlocking {
        val call = companionCall(traceCompanion("javaCompanionParse", javaCaller = true), "parse")
        assertEquals("PROJECT", call["kind"].asText())
        assertEquals("StaticVmId.parse", call["target"].asText())
    }

    fun testNamedCompanionCallNamesItsOuterClass() = runBlocking {
        val call = companionCall(traceCompanion("namedFactory"), "of")
        assertEquals("PROJECT", call["kind"].asText())
        assertEquals("FactoryVmId.of", call["target"].asText())
    }

    fun testOwnCompanionCallIsInternal() = runBlocking {
        val chain = traceCompanion("ownParse")
        val call = companionCall(chain, "parse")
        assertEquals("INTERNAL", call["kind"].asText())
        val node = chain.single { it["methodName"].asText() == "parse" }
        assertEquals("INTERNAL", node["reachedBy"].asText())
    }

    fun testOwnCompanionCallUsesTheSameOuterClassName() = runBlocking {
        val call = companionCall(traceCompanion("ownParse"), "parse")
        assertEquals("VmId.parse", call["target"].asText())
    }

    fun testTopLevelObjectCallKeepsItsName() = runBlocking {
        val call = companionCall(traceCompanion("objectParse"), "parse")
        assertEquals("IdFormat.parse", call["target"].asText())
        assertEquals("PROJECT", call["kind"].asText())
    }

    fun testNestedClassCallKeepsItsShortName() = runBlocking {
        val call = companionCall(traceCompanion("nestedParse"), "parse")
        assertEquals("Inner.parse", call["target"].asText())
        assertEquals("PROJECT", call["kind"].asText())
    }

    fun testCompanionCallsLinkToDistinctOuterClassNodes() = runBlocking {
        val chain = traceCompanion("twoParsers")
        val calls = chain[0]["callsInto"].filter { it["target"].asText().endsWith(".parse") }
        assertEquals("Both companion calls must be present in callsInto", 2, calls.size)
        assertTrue("Both calls must link to expanded nodes: $calls", calls.all { it["node"].isIntegralNumber })
        val nodes = calls.map { call -> chain.single { it["id"].asInt() == call["node"].asInt() } }
        assertEquals(2, nodes.map { it["id"].asInt() }.distinct().size)
        assertEquals(
            setOf("explyt.trace.VmId.Companion", "explyt.trace.OtherVmId.Companion"),
            nodes.map { it["className"].asText() }.toSet()
        )
        assertEquals(listOf("parse", "parse"), nodes.map { it["methodName"].asText() })
        assertTrue(nodes.all { it["filePath"].asText().endsWith("Ids.kt") })
    }

    fun testNestedCompanionCallNamesItsImmediateOuterClass() = runBlocking {
        val chain = traceCompanion("nestedCompanionParse")
        val call = companionCall(chain, "parse")
        assertEquals("InnerId.parse", call["target"].asText())
        assertEquals("PROJECT", call["kind"].asText())
        assertTrue("The nested companion must be expanded", call["node"].isIntegralNumber)
        val node = chain.single { it["id"].asInt() == call["node"].asInt() }
        assertEquals("explyt.trace.Outer.InnerId.Companion", node["className"].asText())
    }

    fun testCompanionNameFallsBackToClassNavigationWithoutKotlinMethodOrigin() = runBlocking {
        companionCall(traceCompanion("kotlinParse"), "parse")
        val companion = JavaPsiFacade.getInstance(project)
            .findClass("explyt.trace.VmId.Companion", GlobalSearchScope.projectScope(project))!!
        val navigation = companion.navigationElement as KtObjectDeclaration
        assertTrue(navigation.isCompanion())
        val method = LightMethodBuilder(companion.manager, companion.language, "parse")
            .setContainingClass(companion)
        assertFalse("The fallback must not have a Kotlin method origin", method is KtLightMethod)
        assertEquals("VmId.parse", CallChainTracer.nameOf(method))
    }

    fun testCompanionTestReferenceKeepsOuterClassTargetAndDirectReferenceLines() = runBlocking {
        val chain = traceCompanion("kotlinParse", includeTests = true)
        val call = companionCall(chain, "parse")
        assertEquals("VmId.parse", call["target"].asText())
        assertTrue("The companion must be expanded", call["node"].isIntegralNumber)
        val node = chain.single { it["id"].asInt() == call["node"].asInt() }
        val references = node["testReferences"]
        assertEquals("A direct test call must be discovered", 1, references.size())
        val reference = references.single()
        assertEquals("companionTests/explyt/trace/VmIdTest.kt", reference["filePath"].asText())
        assertEquals(listOf(4), reference["lines"].map { it.asInt() })
        assertTrue("A direct reference has no interface via", reference["via"] == null || reference["via"].isNull)
    }

    private fun companionCall(chain: JsonNode, methodName: String): JsonNode {
        val calls = chain[0]["callsInto"].filter { it["target"].asText().endsWith(".$methodName") }
        assertEquals("The $methodName call must be present in callsInto: ${chain[0]}", 1, calls.size)
        return calls.single()
    }

    private suspend fun traceCompanion(
        methodName: String,
        javaCaller: Boolean = false,
        includeTests: Boolean = false,
    ): JsonNode {
        val kotlin = """
            package explyt.trace

            class VmId {
                companion object {
                    fun parse(raw: String): String = raw
                }
                fun ownParse(raw: String): String = parse(raw)
            }
            class OtherVmId {
                companion object {
                    fun parse(raw: String): String = raw
                }
            }
            class StaticVmId {
                companion object {
                    @JvmStatic
                    fun parse(raw: String): String = raw
                }
            }
            class FactoryVmId {
                companion object Factory {
                    fun of(raw: String): String = raw
                }
            }
            object IdFormat {
                fun parse(raw: String): String = raw
            }
            class Outer {
                class InnerId {
                    companion object {
                        fun parse(raw: String): String = raw
                    }
                }
                class Inner {
                    fun parse(raw: String): String = raw
                }
            }
            class IdService {
                fun kotlinParse(raw: String): String = VmId.parse(raw)
                fun namedFactory(raw: String): String = FactoryVmId.of(raw)
                fun objectParse(raw: String): String = IdFormat.parse(raw)
                fun nestedParse(raw: String): String = Outer.Inner().parse(raw)
                fun nestedCompanionParse(raw: String): String = Outer.InnerId.parse(raw)
                fun twoParsers(raw: String): String = VmId.parse(raw) + OtherVmId.parse(raw)
            }
        """.trimIndent()
        val java = """
            package explyt.trace;

            public class JavaIdService {
                public String javaStaticParse(String raw) { return StaticVmId.parse(raw); }
                public String javaCompanionParse(String raw) { return StaticVmId.Companion.parse(raw); }
            }
        """.trimIndent()
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            val library = TestLibrary.kotlin_1_9_22
            addFromMaven(model, library.mavenCoordinates, library.includeTransitiveDependencies)
        }
        val sourcesRoot = File(project.basePath!!, "companionSrc").apply { mkdirs() }
        writeSource(sourcesRoot, "explyt/trace/Ids.kt", kotlin)
        writeSource(sourcesRoot, "explyt/trace/JavaIdService.java", java)
        registerSourceRoot(sourcesRoot, isTestSource = false)
        if (includeTests) {
            val testRoot = File(project.basePath!!, "companionTests").apply { mkdirs() }
            writeSource(testRoot, "explyt/trace/VmIdTest.kt", """
                package explyt.trace

                class VmIdTest {
                    fun parses(raw: String): String = VmId.parse(raw)
                }
            """.trimIndent())
            registerSourceRoot(testRoot)
        }
        val source = if (javaCaller) java else kotlin
        val fileName = if (javaCaller) "JavaIdService.java" else "Ids.kt"
        val anchor = source.lines().indexOfFirst { it.contains(" $methodName(") }
        assertTrue("The traced declaration must exist in $fileName", anchor >= 0)
        val result = mapper.readTree(
            toolset.traceCallChain(
                filePath = "companionSrc/explyt/trace/$fileName",
                line = anchor + 1,
                projectPath = project.basePath!!,
                depth = 2,
                includeTests = includeTests,
                limit = 20,
                maxChars = 16000,
            )
        )
        assertEquals("OK", result["status"]?.asText())
        val chain = result["chain"]
        assertEquals(methodName, chain[0]["methodName"].asText())
        return chain
    }

    private fun resolvedCalls(method: UMethod): List<PsiMethod> {
        val result = mutableListOf<PsiMethod>()
        method.accept(object : AbstractUastVisitor() {
            override fun visitCallExpression(node: UCallExpression): Boolean {
                node.resolve()?.let(result::add)
                return false
            }
        })
        return result
    }

    private fun registerSourceRoot(sourcesRoot: File, isTestSource: Boolean = true) {
        WriteAction.runAndWait<Throwable> {
            LocalFileSystem.getInstance().refresh(false)
        }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, isTestSource)
        }
        WriteAction.runAndWait<Throwable> {
            LocalFileSystem.getInstance().refresh(false)
        }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun writeSource(sourcesRoot: File, relativePath: String, content: String) {
        val target = File(sourcesRoot, relativePath)
        target.parentFile.mkdirs()
        target.writeText(content)
    }
}
