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
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootModificationUtil
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.patterns.PlatformPatterns
import com.intellij.pom.java.LanguageLevel
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiReference
import com.intellij.psi.PsiReferenceBase
import com.intellij.psi.PsiReferenceProvider
import com.intellij.psi.PsiReferenceRegistrar
import com.intellij.psi.impl.source.resolve.reference.PsiReferenceRegistrarImpl
import com.intellij.psi.impl.source.resolve.reference.ReferenceProvidersRegistry
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.search.searches.MethodReferencesSearch
import com.intellij.util.ProcessingContext
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

/**
 * Which calls `explyt_trace_spring_call_chain` follows, how it counts depth, and what it reports about each call.
 *
 * The fixture is a Kotlin controller, service and repository shaped like a real one: a KDoc on the handler, same-class
 * helpers, a Kotlin `object` helper, an extension function, calls into `java.time` and the Kotlin standard library, an
 * interface-typed bean, a Spring Data repository with both a declared and an inherited query method, an injected
 * `JdbcTemplate` and an `internal` query builder. Uses the heavy [JavaCodeInsightFixtureTestCase] because
 * `traceCallChain` resolves its file through `LocalFileSystem`, which cannot see the in-memory VFS of a light fixture.
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
        addSource(TEST_ROOT, "com/example/links/ShortLinkInterfaceTest.kt", INTERFACE_TEST_SOURCE, isTestSource = true)
        addSource(TEST_ROOT, "com/example/links/ShortLinkWebTest.kt", WEB_TEST_SOURCE, isTestSource = true)
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
                "ShortLinksKt.minusWindow",
                "WindowFormat.normalize",
                "ShortLinkStatsRepository.activity",
                "ShortLinkStatsRepository.activitySql",
            ),
            chain.map(::nameOf)
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
        val names = traceFromHandler(depth = 1)["chain"].map(::nameOf)

        assertEquals(
            "Depth 1 reaches the service and the object helper through the controller's helpers, and stops before the repository",
            listOf(
                "ShortLinkAdminController.activity",
                "ShortLinkAdminController.parseWindow",
                "ShortLinkAdminController.basisFor",
                "ShortLinkActivityServiceImpl.activity",
                "ShortLinksKt.minusWindow",
                "WindowFormat.normalize",
            ),
            names
        )
    }

    /**
     * `depth` is the number of calls into other classes the trace follows from the start method, as the tool
     * describes it: controller → service → provider → store is three such calls, so `depth: 3` reaches the store.
     * The start method used to be charged a layer of its own, which left the store's node missing at 3 and
     * made it appear only at 4.
     */
    fun testDepthFollowsThatManyCallsIntoOtherClassesFromTheStart() = runBlocking<Unit> {
        val chain = traceLayers(REPORT_HANDLER, depth = 3)["chain"]

        assertEquals(
            listOf(
                "ReportController.report",
                "ReportController.trimmed",
                "ReportService.report",
                "ReportProvider.provide",
                "ReportStore.load",
            ),
            chain.map(::nameOf)
        )
        val callsIntoOtherClasses = chain.flatMap { it["callsInto"] }.filter { it["kind"].asText() == "PROJECT" }
        assertEquals(
            listOf("ReportService.report", "ReportProvider.provide", "ReportStore.load", "AuditLog.record"),
            callsIntoOtherClasses.map { it["target"].asText() }
        )
        assertEquals(
            "The first three calls into other classes reach traced nodes; the fourth lies beyond depth 3",
            listOf(true, true, true, false),
            callsIntoOtherClasses.map { !it["node"].isNull }
        )
    }

    fun testDepthTwoStopsBeforeTheThirdCallIntoAnotherClass() = runBlocking<Unit> {
        val chain = traceLayers(REPORT_HANDLER, depth = 2)["chain"]

        assertEquals(
            listOf("ReportController.report", "ReportController.trimmed", "ReportService.report", "ReportProvider.provide"),
            chain.map(::nameOf)
        )
        val storeCall = chain.single { nameOf(it) == "ReportProvider.provide" }["callsInto"].single()
        assertEquals("ReportStore.load", storeCall["target"].asText())
        assertTrue("The third call into another class lies beyond depth 2", storeCall["node"].isNull)
    }

    /** A private helper between the start method and the service is the same class, so it costs no layer. */
    fun testHelperBetweenTheStartAndTheServiceCostsNoDepth() = runBlocking<Unit> {
        val chain = traceLayers(HELPED_HANDLER, depth = 1)["chain"]

        assertEquals(
            listOf("HelperController.report", "HelperController.viaHelper", "ReportService.report"),
            chain.map(::nameOf)
        )
        val serviceCall = chain.single { nameOf(it) == "HelperController.viaHelper" }["callsInto"].single()
        assertEquals(
            chain.single { nameOf(it) == "ReportService.report" }["id"].asInt(),
            serviceCall["node"].asInt()
        )
    }

    /** A bean injected by its interface is followed to the implementation, where the behaviour actually is. */
    fun testInterfaceCallIsFollowedToItsImplementation() = runBlocking<Unit> {
        val head = traceFromHandler()["chain"][0]

        val serviceCall = head["callsInto"].single { it["target"].asText() == "ShortLinkActivityServiceImpl.activity" }
        assertEquals("ShortLinkActivityService.activity", serviceCall["via"].asText())
        assertEquals("PROJECT", serviceCall["kind"].asText())
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
     * A Spring Data repository has no body to trace, whether the method is declared in the project interface or
     * inherited from `CrudRepository`: both are where the request reaches the database, and both are reported the
     * same way - an external call named after the project repository.
     */
    fun testDeclaredAndInheritedRepositoryMethodsAreReportedAlike() = runBlocking<Unit> {
        val calls = repositoryNode()["callsInto"].associateBy { it["target"].asText() }

        for (target in listOf("ShortLinkRepository.findById", "ShortLinkRepository.findBySlug")) {
            val call = calls[target] ?: error("Expected $target among ${calls.keys}")
            assertEquals("$target is where the request leaves the application", "EXTERNAL", call["kind"].asText())
            assertTrue("$target has no body to trace", call["node"].isNull)
        }
        assertEquals(lineOf(MAIN_SOURCE, "links.findById(id)"), calls.getValue("ShortLinkRepository.findById")["line"].asInt())
    }

    /**
     * A library method called on an injected bean is where the I/O happens - the SQL runs in `JdbcTemplate.query`.
     * Dropping it with the rest of the library calls left the chain without its last, most interesting edge.
     */
    fun testCallOnAnInjectedLibraryBeanIsListedAsExternal() = runBlocking<Unit> {
        val calls = repositoryNode()["callsInto"]

        val query = calls.single { it["target"].asText() == "JdbcTemplate.queryForList" }
        assertEquals("EXTERNAL", query["kind"].asText())
        assertTrue(query["node"].isNull)
        assertEquals(lineOf(MAIN_SOURCE, "jdbc.queryForList("), query["line"].asInt())
        assertEquals(
            "Only the calls where the request leaves the application, and the helper, are listed",
            listOf("ShortLinkRepository.findById", "ShortLinkRepository.findBySlug", "JdbcTemplate.queryForList", "ShortLinkStatsRepository.activitySql"),
            calls.map { it["target"].asText() }
        )
    }

    /** An injected JDK type - a `Clock` - is plumbing: reading the time is not where the request leaves the application. */
    fun testCallOnAnInjectedJdkTypeIsNotListed() = runBlocking<Unit> {
        val service = traceFromHandler()["chain"].single { nameOf(it) == "ShortLinkActivityServiceImpl.activity" }

        assertEquals(
            listOf("ShortLinkStatsRepository.activity", "ShortLinksKt.minusWindow"),
            service["callsInto"].map { it["target"].asText() }
        )
    }

    /**
     * `layer` is the stereotype of the class, which a private helper shares with the bean method calling it; what
     * tells the two apart is how the method was reached. A Kotlin `object` is not a bean and has no layer.
     */
    fun testNodeSaysWhetherItIsAHelperOrABeanEntryPoint() = runBlocking<Unit> {
        val chain = traceFromHandler()["chain"].associateBy(::nameOf)

        assertTrue("The starting method is reached by nothing", chain.getValue("ShortLinkAdminController.activity")["reachedBy"].isNull)
        val helper = chain.getValue("ShortLinkAdminController.parseWindow")
        assertEquals("CONTROLLER", helper["layer"].asText())
        assertEquals("INTERNAL", helper["reachedBy"].asText())
        assertEquals("PROJECT", chain.getValue("ShortLinkActivityServiceImpl.activity")["reachedBy"].asText())
        val objectHelper = chain.getValue("WindowFormat.normalize")
        assertTrue("A Kotlin object is not a bean", objectHelper["layer"].isNull)
        assertEquals("PROJECT", objectHelper["reachedBy"].asText())

        val kinds = chain.getValue("ShortLinkAdminController.parseWindow")["callsInto"]
            .associate { it["target"].asText() to it["kind"].asText() }
        assertEquals(mapOf("ShortLinkAdminController.basisFor" to "INTERNAL", "WindowFormat.normalize" to "PROJECT"), kinds)
    }

    /**
     * The JVM signature of a Kotlin extension function carries its receiver as `$this$minusWindow`, a parameter no
     * caller passes and nobody can search for.
     */
    fun testParametersAreTheDeclaredOnes() = runBlocking<Unit> {
        val chain = traceFromHandler()["chain"].associateBy(::nameOf)

        assertEquals(listOf("window"), chain.getValue("ShortLinksKt.minusWindow")["parameters"].map { it.asText() })
        assertEquals(listOf("id", "window"), chain.getValue("ShortLinkAdminController.activity")["parameters"].map { it.asText() })
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
        val chain = traceFromHandler(includeTests = true)["chain"]

        val referenced = chain.filter { node -> node["testReferences"].any { it["via"].isNull } }.map(::nameOf)
        assertEquals(listOf("ShortLinkActivityServiceImpl.activity"), referenced)
        assertEquals(
            "A reference is reported on the node of the method it names",
            listOf("$TEST_ROOT/com/example/links/ShortLinkActivityTest.kt"),
            implementationNode(chain)["testReferences"].filter { it["via"].isNull }.map { it["filePath"].asText() }
        )
        assertEquals(
            "The test calls the implementation once",
            listOf(lineOf(TEST_SOURCE, "service.activity(1, Duration.ofDays(7))")),
            implementationNode(chain)["testReferences"].single { it["via"].isNull }["lines"].map { it.asInt() }
        )
    }

    /**
     * A test written against the interface the bean is injected by - a caller or a mock of it - breaks as surely when
     * the implementation's signature changes, although it never names the implementation.
     */
    fun testTestWrittenAgainstTheInterfaceIsListedOnTheImplementation() = runBlocking<Unit> {
        val references = implementationNode(traceFromHandler(includeTests = true)["chain"])["testReferences"]

        val throughInterfaces = references.filter { !it["via"].isNull }
        assertEquals(
            "The interface-typed test is listed once, through the interface, got $references",
            1, throughInterfaces.size
        )
        val throughInterface = throughInterfaces.single()
        assertEquals("ShortLinkActivityService.activity", throughInterface["via"].asText())
        assertEquals("$TEST_ROOT/com/example/links/ShortLinkInterfaceTest.kt", throughInterface["filePath"].asText())
        assertEquals(
            listOf(lineOf(INTERFACE_TEST_SOURCE, "service.activity(2, Duration.ofDays(1))")),
            throughInterface["lines"].map { it.asInt() }
        )
    }

    /** A web test reaches a handler through its URL, never by calling it, so no reference search finds it. */
    fun testWebTestCallingTheHandlerUrlIsListedOnTheHandler() = runBlocking<Unit> {
        val chain = traceFromHandler(includeTests = true)["chain"]

        val byUrl = chain[0]["testUrlReferences"]
        assertEquals(listOf("$TEST_ROOT/com/example/links/ShortLinkWebTest.kt"), byUrl.map { it["filePath"].asText() })
        assertEquals("/api/short-links/{id}/activity", byUrl.single()["endpointPath"].asText())
        assertEquals(
            listOf(lineOf(WEB_TEST_SOURCE, "get(\"/api/short-links/1/activity\")")),
            byUrl.single()["lines"].map { it.asInt() }
        )
        assertTrue(
            "Only the handler the trace started from is matched by URL",
            chain.drop(1).none { it.has("testUrlReferences") }
        )
    }

    /**
     * A request found by its URL is reported once, under `testUrlReferences`, also when the URL string carries a
     * reference that resolves to the handler - the bundled JetBrains Spring MVC plugin puts one there, so in IntelliJ
     * IDEA Ultimate the reference search returned the request line too. The test sandbox disables that plugin, so the
     * fixture registers a reference of the same shape itself.
     */
    fun testRequestFoundByUrlIsNotRepeatedAsAReference() = runBlocking<Unit> {
        registerUrlReferencesToTheHandler()
        val requestLine = lineOf(WEB_TEST_SOURCE, "get(\"/api/short-links/1/activity\")")
        val webTest = "$TEST_ROOT/com/example/links/ShortLinkWebTest.kt"
        assertTrue(
            "Precondition: the reference search returns the URL string of the request",
            urlStringReferencesToTheHandler().isNotEmpty()
        )

        val head = traceFromHandler(includeTests = true)["chain"][0]
        assertEquals(
            "The request is listed by URL",
            listOf(requestLine),
            head["testUrlReferences"].single { it["filePath"].asText() == webTest }["lines"].map { it.asInt() }
        )
        val repeated = head["testReferences"]
            .filter { it["filePath"].asText() == webTest && it["lines"].any { line -> line.asInt() == requestLine } }
        assertEquals("A request found by URL must not be listed again as a reference", emptyList<JsonNode>(), repeated)
    }

    /**
     * A MockMvc request never leaves the JVM: the host it names - a brand, a tenant - becomes the request's server
     * name, so the request still reaches the handler under test.
     */
    fun testMockMvcRequestToAnyHostIsListedOnTheHandler() = runBlocking<Unit> {
        addSource(BRAND_ROOT, "com/example/links/ShortLinkBrandTest.kt", BRAND_TEST_SOURCE, isTestSource = true)

        val head = traceFromHandler(includeTests = true)["chain"][0]

        assertEquals(
            listOf(lineOf(BRAND_TEST_SOURCE, "brand-a.example"), lineOf(BRAND_TEST_SOURCE, ".also { controller")),
            head["testUrlReferences"].single { it["filePath"].asText() == BRAND_TEST }["lines"].map { it.asInt() }
        )
    }

    /**
     * A `RANDOM_PORT` test sends its request over the network to this machine, so it reaches the handler; the same
     * request to another host is a call to another service.
     */
    fun testRealClientToThisMachineIsListedOnTheHandler() = runBlocking<Unit> {
        addSource(BRAND_ROOT, "com/example/links/ShortLinkClientTest.kt", CLIENT_TEST_SOURCE, isTestSource = true)

        val head = traceFromHandler(includeTests = true)["chain"][0]

        assertEquals(
            listOf(lineOf(CLIENT_TEST_SOURCE, "localhost:")),
            head["testUrlReferences"].single { it["filePath"].asText() == CLIENT_TEST }["lines"].map { it.asInt() }
        )
    }

    /**
     * Dropping a request from the references must not drop a call of the handler: a test calling it directly breaks
     * on a signature change, also when a request to its URL sits on the same line.
     */
    fun testDirectCallOfTheHandlerStaysAReference() = runBlocking<Unit> {
        addSource(BRAND_ROOT, "com/example/links/ShortLinkBrandTest.kt", BRAND_TEST_SOURCE, isTestSource = true)
        registerUrlReferencesToTheHandler()

        val head = traceFromHandler(includeTests = true)["chain"][0]

        assertEquals(
            listOf(lineOf(BRAND_TEST_SOURCE, "controller.activity(1, \"7d\")"), lineOf(BRAND_TEST_SOURCE, ".also { controller")),
            head["testReferences"].single { it["filePath"].asText() == BRAND_TEST && it["via"].isNull }["lines"].map { it.asInt() }
        )
    }

    /**
     * Gives every Kotlin string naming a `/api/short-links/` URL a reference to the handler, the way the bundled
     * JetBrains Spring MVC plugin does for a MockMvc URL: it resolves to the handler and is a reference to it.
     */
    private fun registerUrlReferencesToTheHandler() {
        val registrar = ReferenceProvidersRegistry.getInstance().getRegistrar(KotlinLanguage.INSTANCE) as PsiReferenceRegistrarImpl
        registrar.registerReferenceProvider(
            PlatformPatterns.psiElement(KtStringTemplateExpression::class.java),
            object : PsiReferenceProvider() {
                override fun getReferencesByElement(element: PsiElement, context: ProcessingContext): Array<PsiReference> {
                    val template = element as KtStringTemplateExpression
                    if (!template.text.contains("/api/short-links/")) return PsiReference.EMPTY_ARRAY
                    return arrayOf(UrlToHandlerReference(template))
                }
            },
            PsiReferenceRegistrar.DEFAULT_PRIORITY,
            testRootDisposable,
        )
    }

    private fun urlStringReferencesToTheHandler(): List<PsiElement> =
        MethodReferencesSearch.search(handler(project), GlobalSearchScope.projectScope(project), true).findAll()
            .map { it.element }
            .filterIsInstance<KtStringTemplateExpression>()

    private class UrlToHandlerReference(template: KtStringTemplateExpression) :
        PsiReferenceBase<KtStringTemplateExpression>(template, TextRange(1, template.textLength - 1)) {

        override fun resolve(): PsiElement = handler(element.project)

        override fun isReferenceTo(element: PsiElement): Boolean =
            element.manager.areElementsEquivalent(element.navigationElement, resolve().navigationElement)
    }

    /** Absent means "not requested"; an empty list would claim that no test depends on the method. */
    fun testTestFieldsAreAbsentWhenTestsAreNotRequested() = runBlocking<Unit> {
        val chain = traceFromHandler(includeTests = false)["chain"]

        assertTrue(
            "Test fields must be absent when tests are not requested, got ${chain.filter { it.has("testReferences") }.map(::nameOf)}",
            chain.none { it.has("testReferences") || it.has("testUrlReferences") }
        )
        assertTrue(
            "Requested and not found is an empty list",
            traceFromHandler(includeTests = true)["chain"].all { it["testReferences"].isArray }
        )
    }

    /**
     * The proxy annotations of a method are what makes a call behave differently from its body: a transaction, a
     * cache hit, another thread. They are read from the method and from its class, and a project annotation carrying
     * one as a meta-annotation is reported as the annotation it stands for.
     */
    fun testProxyAnnotationsAreListedWithWhereTheyAreDeclared() = runBlocking<Unit> {
        val chain = traceFromHandler()["chain"]

        assertEquals(
            listOf("org.springframework.transaction.annotation.Transactional" to "CLASS"),
            aopOf(implementationNode(chain))
        )
        assertEquals(
            listOf(
                "org.springframework.cache.annotation.Cacheable" to "METHOD",
                "org.springframework.transaction.annotation.Transactional" to "METHOD",
            ),
            aopOf(chain.single { nameOf(it) == "ShortLinkStatsRepository.activity" })
        )
        assertEquals(emptyList<Pair<String, String>>(), aopOf(chain[0]))
    }

    private fun aopOf(node: JsonNode): List<Pair<String, String>> =
        node["aop"].map { it["annotation"].asText() to it["declaredOn"].asText() }

    private fun implementationNode(chain: JsonNode): JsonNode =
        chain.single { nameOf(it) == "ShortLinkActivityServiceImpl.activity" }

    /**
     * A chain stopped by its size limit says so, rather than reading as a request path that ends there. The tool
     * reports it as `chainLimitReached`, separate from the `truncated` of a page that continues.
     */
    fun testChainCutAtItsSizeLimitIsReported() {
        val handler = JavaPsiFacade.getInstance(project)
            .findClass("com.example.links.ShortLinkAdminController", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("activity", false).single()

        val cut = CallChainTracer(project, maxMethods = 2).trace(handler, depth = 3)
        assertEquals(2, cut.methods.size)
        assertTrue(cut.truncated)

        val whole = CallChainTracer(project, maxMethods = 50).trace(handler, depth = 3)
        assertEquals(8, whole.methods.size)
        assertFalse(whole.truncated)
    }

    /**
     * The tool reports the cut too, not only the tracer: a chain of same-class helpers costs no depth, so a class
     * calling sixty of them in a row reaches the method cap.
     */
    fun testChainCutAtItsSizeLimitIsReportedByTheTool() = runBlocking<Unit> {
        addSource(LONG_ROOT, "com/example/links/LongChain.kt", LONG_CHAIN_SOURCE)

        val root = mapper.readTree(
            toolset.traceCallChain(
                filePath = "$LONG_ROOT/com/example/links/LongChain.kt",
                line = lineOf(LONG_CHAIN_SOURCE, "fun start()"),
                projectPath = project.basePath!!,
                includeTests = false,
            )
        )

        assertEquals("The chain stops at the 50-method cap", 50, root["totalCount"].asInt())
        assertTrue("A chain cut at the cap must say so", root["chainLimitReached"].asBoolean())
    }

    /** The defaults hold an ordinary controller-to-repository chain, test references included, in one page. */
    fun testDefaultPageHoldsTheWholeChain() = runBlocking<Unit> {
        val root = traceFromHandler(includeTests = true)

        assertEquals("OK", root["status"].asText())
        assertEquals(8, root["totalCount"].asInt())
        assertEquals((0 until 8).toList(), root["chain"].map { it["id"].asInt() })
        assertFalse(root["truncated"].asBoolean())
        assertTrue(root["nextOffset"].isNull)
        assertFalse("The fixture is far below the method cap", root["chainLimitReached"].asBoolean())
    }

    /**
     * A long chain is read in pages: each continuation names the revision of the first page, and together the pages
     * serve every node exactly once, in chain order, so a `node` id on one page resolves on another.
     */
    fun testPagesContinueThroughTheWholeChain() = runBlocking<Unit> {
        val first = tracePage(limit = 3)
        assertTrue("Precondition: the chain must be longer than one page", first["truncated"].asBoolean())

        val ids = mutableListOf<Int>()
        var page = first
        while (true) {
            ids += page["chain"].map { it["id"].asInt() }
            if (!page["truncated"].asBoolean()) break
            page = tracePage(limit = 3, offset = page["nextOffset"].asInt(), expectedRevision = first["revision"].asText())
        }

        assertEquals((0 until first["totalCount"].asInt()).toList(), ids)
    }

    /** A page is measured on the finished answer, so a small budget ends it early instead of cutting a node. */
    fun testCharacterBudgetEndsThePageBeforeTheLimit() = runBlocking<Unit> {
        val root = tracePage(limit = 50, maxChars = 1200)

        assertTrue(root["truncated"].asBoolean())
        assertTrue("At least one node fits 1200 chars", root["chain"].size() in 1 until 8)
        assertTrue(mapper.writeValueAsString(root).length <= 1200)
    }

    /** A revision belongs to one query: continuing a different trace with it would stitch two chains together. */
    fun testRevisionOfAnotherQueryIsRejected() = runBlocking<Unit> {
        val first = tracePage(limit = 3)

        val other = tracePage(limit = 3, offset = 3, expectedRevision = first["revision"].asText(), depth = 2)

        assertEquals("ERROR", other["status"].asText())
        assertEquals("RESULT_CHANGED", other["error"]["code"].asText())
    }

    fun testContinuationWithoutRevisionIsRejected() = runBlocking<Unit> {
        val root = tracePage(limit = 3, offset = 3)

        assertEquals("ERROR", root["status"].asText())
        assertEquals("INVALID_ARGUMENT", root["error"]["code"].asText())
    }

    private suspend fun tracePage(
        limit: Int,
        offset: Int = 0,
        maxChars: Int = 16000,
        expectedRevision: String? = null,
        depth: Int = 3,
    ): JsonNode = mapper.readTree(
        toolset.traceCallChain(
            filePath = "$MAIN_ROOT/com/example/links/ShortLinks.kt",
            line = lineOf(MAIN_SOURCE, "fun activity(@PathVariable"),
            projectPath = project.basePath!!,
            depth = depth,
            includeTests = false,
            offset = offset,
            limit = limit,
            maxChars = maxChars,
            expectedRevision = expectedRevision,
        )
    )

    private suspend fun repositoryNode(): JsonNode =
        traceFromHandler()["chain"].single { nameOf(it) == "ShortLinkStatsRepository.activity" }

    private suspend fun traceLayers(handlerAnchor: String, depth: Int): JsonNode {
        addSource(LAYERS_ROOT, "com/example/reports/Reports.kt", LAYERS_SOURCE)
        return mapper.readTree(
            toolset.traceCallChain(
                filePath = "$LAYERS_ROOT/com/example/reports/Reports.kt",
                line = lineOf(LAYERS_SOURCE, handlerAnchor),
                projectPath = project.basePath!!,
                depth = depth,
                includeTests = false,
            )
        )
    }

    private fun nameOf(node: JsonNode): String =
        "${node["className"].asText().substringAfterLast('.')}.${node["methodName"].asText()}"

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
        const val LONG_ROOT = "traceLong"
        const val LAYERS_ROOT = "traceLayers"
        const val REPORT_HANDLER = "= service.report(trimmed(name))"
        const val HELPED_HANDLER = "= viaHelper(name)"
        const val BRAND_ROOT = "traceBrandTest"
        const val BRAND_TEST = "$BRAND_ROOT/com/example/links/ShortLinkBrandTest.kt"
        const val CLIENT_TEST = "$BRAND_ROOT/com/example/links/ShortLinkClientTest.kt"

        fun handler(project: Project): PsiMethod =
            JavaPsiFacade.getInstance(project)
                .findClass("com.example.links.ShortLinkAdminController", GlobalSearchScope.allScope(project))!!
                .findMethodsByName("activity", false).single()

        val LONG_CHAIN_SOURCE = buildString {
            appendLine("package com.example.links")
            appendLine()
            appendLine("class LongChain {")
            appendLine("    fun start(): Int = step1()")
            for (step in 1 until 60) appendLine("    private fun step$step(): Int = step${step + 1}()")
            appendLine("    private fun step60(): Int = 0")
            appendLine("}")
        }

        val LAYERS_SOURCE = """
            package com.example.reports

            import org.springframework.stereotype.Component
            import org.springframework.stereotype.Repository
            import org.springframework.stereotype.Service
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RestController

            @Component
            class AuditLog {
                fun record(entry: String): String = entry
            }

            @Repository
            class ReportStore(private val audit: AuditLog) {
                fun load(name: String): String = audit.record(name)
            }

            @Component
            class ReportProvider(private val store: ReportStore) {
                fun provide(name: String): String = store.load(name)
            }

            @Service
            class ReportService(private val provider: ReportProvider) {
                fun report(name: String): String = provider.provide(name)
            }

            @RestController
            class ReportController(private val service: ReportService) {
                @GetMapping("/reports")
                fun report(name: String): String = service.report(trimmed(name))

                private fun trimmed(name: String): String = name.trim()
            }

            @RestController
            class HelperController(private val service: ReportService) {
                @GetMapping("/helped-reports")
                fun report(name: String): String = viaHelper(name)

                private fun viaHelper(name: String): String = service.report(name)
            }
        """.trimIndent()

        val LIBRARIES = listOf(
            TestLibrary.springWebMvc_6_0_7,
            TestLibrary.springDataJpa_3_1_0,
            TestLibrary.springJdbc_6_2_5,
            TestLibrary.springTx_6_0_7,
            TestLibrary.springContext_6_0_7,
            TestLibrary.springTest_6_0_7,
            TestLibrary.kotlin_1_9_22,
        )

        val MAIN_SOURCE = """
            package com.example.links

            import java.time.Clock
            import java.time.Duration
            import java.time.Instant
            import org.springframework.cache.annotation.Cacheable
            import org.springframework.data.repository.CrudRepository
            import org.springframework.jdbc.core.JdbcTemplate
            import org.springframework.stereotype.Repository
            import org.springframework.stereotype.Service
            import org.springframework.transaction.annotation.Transactional
            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.PathVariable
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RequestParam
            import org.springframework.web.bind.annotation.RestController

            class ShortLink(val id: Long, val slug: String)

            interface ShortLinkRepository : CrudRepository<ShortLink, Long> {
                fun findBySlug(slug: String): ShortLink?
            }

            object WindowFormat {
                fun normalize(window: String): String = window.trim().lowercase()
            }

            fun Instant.minusWindow(window: Duration): Instant = minus(window)

            @Transactional(readOnly = true)
            annotation class ReadOnlyQuery

            @Repository
            class ShortLinkStatsRepository(private val links: ShortLinkRepository, private val jdbc: JdbcTemplate) {
                @Cacheable("activity")
                @ReadOnlyQuery
                fun activity(id: Long, since: Instant): List<String> {
                    val link = links.findById(id).orElseThrow()
                    val canonical = links.findBySlug(link.slug)
                    return jdbc.queryForList(activitySql(canonical?.slug ?: link.slug, since), String::class.java)
                }

                internal fun activitySql(slug: String, since: Instant): String = "select * from activity where slug = '${'$'}slug'"
            }

            interface ShortLinkActivityService {
                fun activity(id: Long, window: Duration): List<String>
            }

            @Service
            @Transactional
            class ShortLinkActivityServiceImpl(
                private val stats: ShortLinkStatsRepository,
                private val clock: Clock,
            ) : ShortLinkActivityService {
                override fun activity(id: Long, window: Duration): List<String> =
                    stats.activity(id, clock.instant().minusWindow(window))
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

                private fun parseWindow(window: String): Duration = basisFor(WindowFormat.normalize(window))

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

        val INTERFACE_TEST_SOURCE = """
            package com.example.links

            import java.time.Duration

            class ShortLinkInterfaceTest {
                fun activityOfADay(service: ShortLinkActivityService) {
                    service.activity(2, Duration.ofDays(1))
                }
            }
        """.trimIndent()

        val WEB_TEST_SOURCE = """
            package com.example.links

            import org.springframework.test.web.servlet.MockMvc
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ShortLinkWebTest {
                fun activityOverHttp(mockMvc: MockMvc) {
                    mockMvc.perform(get("/api/short-links/1/activity"))
                }
            }
        """.trimIndent()

        val BRAND_TEST_SOURCE = """
            package com.example.links

            import java.net.URI
            import org.springframework.test.web.servlet.MockMvc
            import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get

            class ShortLinkBrandTest {
                fun activityOfBrandA(mockMvc: MockMvc) {
                    mockMvc.perform(get(URI.create("http://brand-a.example/api/short-links/1/activity")))
                }

                fun activityCalledDirectly(controller: ShortLinkAdminController) {
                    controller.activity(1, "7d")
                }

                fun requestAndCallOnOneLine(mockMvc: MockMvc, controller: ShortLinkAdminController) {
                    mockMvc.perform(get("/api/short-links/2/activity")).also { controller.activity(2, "1d") }
                }
            }
        """.trimIndent()

        val CLIENT_TEST_SOURCE = """
            package com.example.links

            import org.springframework.web.client.RestTemplate

            class ShortLinkClientTest {
                fun activityOverTheNetwork(rest: RestTemplate, port: Int) {
                    rest.getForObject("http://localhost:${'$'}port/api/short-links/3/activity", String::class.java)
                    rest.getForObject("http://payments.example.com/api/short-links/3/activity", String::class.java)
                }
            }
        """.trimIndent()
    }
}
