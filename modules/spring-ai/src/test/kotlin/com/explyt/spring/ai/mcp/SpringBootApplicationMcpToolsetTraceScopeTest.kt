/*
 * Copyright (c) 2026 Explyt Ltd
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
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Which calls `explyt_trace_spring_call_chain` follows, how it counts depth, and what it reports about each call.
 *
 * The fixture is a Kotlin controller, service and repository shaped like a real one: a KDoc on the handler, same-class
 * helpers, calls into `java.time` and the Kotlin standard library, an interface-typed bean, a Spring Data repository
 * and an `internal` query builder. Uses the heavy [JavaCodeInsightFixtureTestCase] because `traceCallChain` resolves
 * its file through `LocalFileSystem`, which cannot see the in-memory VFS of a light fixture.
 */
class SpringBootApplicationMcpToolsetTraceScopeTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            LIBRARIES.forEach { addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies) }
        }
        addSource(MAIN_ROOT, "com/example/links/ShortLinks.kt", MAIN_SOURCE)
        addSource(TEST_ROOT, "com/example/links/ShortLinkActivityTest.kt", TEST_SOURCE, isTestSource = true)
    }

    /**
     * Only project methods are traced. `Instant.now`, `trim` and `buildList` name nothing the caller can change, and
     * listing them as chain nodes buried the handful of methods that make up the request path.
     */
    fun testChainHoldsProjectMethodsOnly() = runBlocking<Unit> {
        val chain = traceFromHandler()["chain"]

        assertEquals(
            listOf(
                "ShortLinkAdminController.activity",
                "ShortLinkAdminController.parseWindow",
                "ShortLinkAdminController.basisFor",
                "ShortLinkActivityServiceImpl.activity",
                "ShortLinkStatsRepository.activity",
                "ShortLinkStatsRepository.activitySql",
            ),
            chain.map { "${it["className"].asText().substringAfterLast('.')}.${it["methodName"].asText()}" }
        )
        assertTrue(
            "No chain node may point into a jar or the JDK, got ${chain.map { it["filePath"] }}",
            chain.all { it["filePath"].asText().startsWith("$MAIN_ROOT/") }
        )
    }

    /**
     * A private helper of the controller is still the controller. Charging a layer for it stopped a depth-3 trace
     * inside the repository, before the SQL-building method where the defect behind the original report was.
     */
    fun testDepthCountsCallsIntoOtherClassesOnly() = runBlocking<Unit> {
        val names = traceFromHandler(depth = 2)["chain"].map { it["methodName"].asText() }

        assertEquals(
            "Depth 2 reaches the service and stops before the repository",
            listOf("activity", "parseWindow", "basisFor", "activity"),
            names
        )
    }

    /** A bean injected by its interface is followed to the implementation, where the behaviour actually is. */
    fun testInterfaceCallIsFollowedToItsImplementation() = runBlocking<Unit> {
        val head = traceFromHandler()["chain"][0]

        val serviceCall = head["callsInto"].single { it["target"].asText() == "ShortLinkActivityServiceImpl.activity" }
        assertEquals("ShortLinkActivityService.activity", serviceCall["via"].asText())
        assertEquals(3, serviceCall["node"].asInt())
    }

    /**
     * A call reports the line it is made on, which is where a new argument is threaded through - not the callee's
     * declaration line, which for a framework method was a line inside a jar.
     */
    fun testCallReportsItsCallSiteLine() = runBlocking<Unit> {
        val head = traceFromHandler()["chain"][0]

        val serviceCall = head["callsInto"].single { it["target"].asText() == "ShortLinkActivityServiceImpl.activity" }
        assertEquals(lineOf(MAIN_SOURCE, "service.activity(id, parseWindow(window))"), serviceCall["line"].asInt())
    }

    /**
     * A Spring Data repository method is declared in a framework jar, yet it is exactly the repository layer of a
     * Spring Data application: it is listed under the project type it is called on, and not followed into the jar.
     */
    fun testSpringDataRepositoryMethodIsListedButNotTraced() = runBlocking<Unit> {
        val repository = traceFromHandler()["chain"]
            .single { it["className"].asText().endsWith("ShortLinkStatsRepository") && it["methodName"].asText() == "activity" }

        val findById = repository["callsInto"].single { it["target"].asText() == "ShortLinkRepository.findById" }
        assertTrue("A framework method is not a chain node", findById["node"].isNull)
        assertEquals(lineOf(MAIN_SOURCE, "links.findById(id)"), findById["line"].asInt())
        assertTrue(
            "Calls into the JDK and the standard library are not listed, got ${repository["callsInto"]}",
            repository["callsInto"].none { it["target"].asText().run { startsWith("Instant.") || startsWith("StringsKt.") } }
        )
    }

    /** A Kotlin `internal` function is reported by its source name, not by the mangled JVM name nobody can search. */
    fun testInternalFunctionKeepsItsSourceName() = runBlocking<Unit> {
        val chain = traceFromHandler()["chain"]

        assertTrue(chain.any { it["methodName"].asText() == "activitySql" })
        assertTrue(
            "No mangled name may leak, got ${chain.map { it["methodName"] }}",
            chain.none { it["methodName"].asText().contains('$') }
        )
    }

    /**
     * Test references are what a signature change breaks. Searching them for `Instant.now` as well listed every test
     * of the project that reads the clock; only tests calling a traced project method belong here.
     */
    fun testTestReferencesCoverTracedProjectMethodsOnly() = runBlocking<Unit> {
        val references = traceFromHandler(includeTests = true)["testReferences"]

        assertEquals(listOf("$TEST_ROOT/com/example/links/ShortLinkActivityTest.kt"), references.map { it["filePath"].asText() })
        assertEquals(
            listOf("ShortLinkActivityServiceImpl.activity"),
            references.single()["referencedMethods"].map { it["method"].asText() }
        )
    }

    /** A chain stopped by its size limit says so, rather than reading as a request path that ends there. */
    fun testChainCutAtItsSizeLimitIsReportedTruncated() {
        val handler = JavaPsiFacade.getInstance(project)
            .findClass("com.example.links.ShortLinkAdminController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("activity", false).single()

        val cut = CallChainTracer(project, maxMethods = 2).trace(handler, depth = 3)
        assertEquals(2, cut.methods.size)
        assertTrue(cut.truncated)

        val whole = CallChainTracer(project, maxMethods = 50).trace(handler, depth = 3)
        assertEquals(6, whole.methods.size)
        assertFalse(whole.truncated)
    }

    private suspend fun traceFromHandler(depth: Int = 3, includeTests: Boolean = false): JsonNode = mapper.readTree(
        toolset.traceCallChain(
            filePath = "$MAIN_ROOT/com/example/links/ShortLinks.kt",
            line = lineOf(MAIN_SOURCE, "fun activity(@PathVariable"),
            projectPath = project.basePath!!,
            depth = depth,
            includeTests = includeTests,
        )
    )

    private fun lineOf(source: String, anchor: String): Int {
        val index = source.lines().indexOfFirst { it.contains(anchor) }
        assertTrue("Anchor '$anchor' is absent from the fixture", index >= 0)
        return index + 1
    }

    private fun addSource(rootName: String, relativePath: String, content: String, isTestSource: Boolean = false) {
        val sourcesRoot = File(project.basePath!!, rootName).apply { mkdirs() }
        File(sourcesRoot, relativePath).apply {
            parentFile.mkdirs()
            writeText(content)
        }

        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val sourcesRootVf = VfsUtil.findFile(sourcesRoot.toPath(), true)
            ?: error("Sources root not visible in VFS: ${sourcesRoot.absolutePath}")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(sourcesRootVf).addSourceFolder(sourcesRootVf, isTestSource)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private companion object {
        const val MAIN_ROOT = "traceMain"
        const val TEST_ROOT = "traceTest"

        val LIBRARIES = listOf(
            TestLibrary.springWebMvc_6_0_7,
            TestLibrary.springDataJpa_3_1_0,
            TestLibrary.kotlin_1_9_22,
        )

        val MAIN_SOURCE = """
            package com.example.links

            import java.time.Duration
            import java.time.Instant
            import org.springframework.data.repository.CrudRepository
            import org.springframework.stereotype.Repository
            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            class ShortLink(val id: Long, val slug: String)

            interface ShortLinkRepository : CrudRepository<ShortLink, Long>

            @Repository
            class ShortLinkStatsRepository(private val links: ShortLinkRepository) {
                fun activity(id: Long, since: Instant): List<String> {
                    val link = links.findById(id).orElseThrow()
                    return listOf(activitySql(link.slug.trim(), since))
                }

                internal fun activitySql(slug: String, since: Instant): String = "select * from activity where slug = '${'$'}slug'"
            }

            interface ShortLinkActivityService {
                fun activity(id: Long, window: Duration): List<String>
            }

            @Service
            class ShortLinkActivityServiceImpl(private val stats: ShortLinkStatsRepository) : ShortLinkActivityService {
                override fun activity(id: Long, window: Duration): List<String> =
                    stats.activity(id, Instant.now().minus(window))
            }

            @RestController
            @RequestMapping("/api/short-links")
            class ShortLinkAdminController(private val service: ShortLinkActivityService) {

                /**
                 * Activity of one short link over a window.
                 */
                @GetMapping("/{id}/activity")
                fun activity(@PathVariable id: Long, @RequestParam(defaultValue = "7d") window: String): List<String> {
                    val rows = service.activity(id, parseWindow(window))
                    return buildList { addAll(rows) }
                }

                private fun parseWindow(window: String): Duration = basisFor(window.trim())

                private fun basisFor(window: String): Duration = Duration.parse("PT" + window.uppercase())
            }
        """.trimIndent()

        val TEST_SOURCE = """
            package com.example.links

            import java.time.Duration
            import java.time.Instant

            class ShortLinkActivityTest {
                fun activityOfAWeek(service: ShortLinkActivityServiceImpl) {
                    Instant.now()
                    service.activity(1, Duration.ofDays(7))
                }
            }
        """.trimIndent()
    }
}
