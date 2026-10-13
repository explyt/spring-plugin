/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.node.ObjectNode
import com.intellij.mcpserver.annotations.McpTool
import kotlin.reflect.KParameter
import kotlin.reflect.full.callSuspendBy
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.instanceParameter

/**
 * The three endpoint tools answer within a character budget and continue from where a page ended, like the bean,
 * entity and trace tools: one answer an agent cannot receive whole is cut by the client, silently.
 *
 * Tools are called by name with named arguments, the way an MCP client calls them, so an argument a tool does not
 * declare fails as the client would see it.
 */
class SpringBootApplicationMcpToolsetEndpointPageTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "mcp/"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWebMvc_6_0_7,
        TestLibrary.kotlin_1_9_22,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        myFixture.copyDirectoryToProject("springBootApp", "")
    }

    fun testFindServesAPageWithinItsBudget() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(FIND, "urlPattern" to DEMO_URL)
        val maxChars = budgetForOneRecord(unbounded)

        val json = call(FIND, "urlPattern" to DEMO_URL, "maxChars" to maxChars)
        val page = mapper.readTree(json)

        assertOkPage(page)
        assertTrue("The finished answer must fit $maxChars, got ${json.length}", json.length <= maxChars)
        assertTrue("A cut page continues", page["truncated"].asBoolean())
        assertEquals(page["endpoints"].size(), page["nextOffset"].asInt())
        assertEquals(unbounded["totalCount"].asInt(), page["totalCount"].asInt())
    }

    fun testFindContinuationServesEveryEndpointOnceInTheUnboundedOrder() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(FIND, "urlPattern" to DEMO_URL)
        val maxChars = budgetForOneRecord(unbounded)

        val pages = followPages(maxChars) { offset, revision ->
            listOfNotNull(
                "urlPattern" to DEMO_URL,
                "maxChars" to maxChars,
                "offset" to offset,
                revision?.let { "expectedRevision" to it },
            ).let { callJson(FIND, *it.toTypedArray()) }
        }

        assertTrue("Precondition: the budget must split the answer, got ${pages.size} page", pages.size > 1)
        assertEquals(identities(unbounded["endpoints"]), pages.flatMap { identities(it["endpoints"]) })
    }

    fun testFindRejectsARevisionOfAnotherQuery() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(FIND, "urlPattern" to DEMO_URL)
        val maxChars = budgetForOneRecord(unbounded)
        val first = callJson(FIND, "urlPattern" to DEMO_URL, "maxChars" to maxChars)
        assertOkPage(first)
        val revision = first["revision"].asText()

        val otherUrl = callJson(
            FIND, "urlPattern" to ROUTES_URL, "maxChars" to maxChars, "offset" to 1, "expectedRevision" to revision
        )
        val otherMethod = callJson(
            FIND, "urlPattern" to DEMO_URL, "httpMethod" to "GET",
            "maxChars" to maxChars, "offset" to 1, "expectedRevision" to revision
        )

        assertResultChanged("another urlPattern", otherUrl)
        assertResultChanged("another httpMethod", otherMethod)
    }

    fun testCompactListingServesAPageWithinItsBudget() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(LIST, "controllerFilter" to WEB_PACKAGE, "compact" to true)
        val maxChars = budgetForOneRecord(unbounded)

        val json = call(LIST, "controllerFilter" to WEB_PACKAGE, "compact" to true, "maxChars" to maxChars)
        val page = mapper.readTree(json)

        assertOkPage(page)
        assertTrue("The finished answer must fit $maxChars, got ${json.length}", json.length <= maxChars)
        assertTrue("A cut page continues", page["truncated"].asBoolean())
        assertEquals(page["endpoints"].size(), page["nextOffset"].asInt())
    }

    fun testCompactListingContinuationServesEveryEndpointOnceInTheUnboundedOrder() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(LIST, "controllerFilter" to WEB_PACKAGE, "compact" to true)
        val maxChars = budgetForOneRecord(unbounded)

        val pages = followPages(maxChars) { offset, revision ->
            listOfNotNull(
                "controllerFilter" to WEB_PACKAGE,
                "compact" to true,
                "maxChars" to maxChars,
                "offset" to offset,
                revision?.let { "expectedRevision" to it },
            ).let { callJson(LIST, *it.toTypedArray()) }
        }

        assertTrue("Precondition: the budget must split the answer, got ${pages.size} page", pages.size > 1)
        assertEquals(identities(unbounded["endpoints"]), pages.flatMap { identities(it["endpoints"]) })
    }

    fun testListingRejectsARevisionOfAnotherQuery() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(LIST, "controllerFilter" to WEB_PACKAGE, "compact" to true)
        val maxChars = budgetForOneRecord(unbounded)
        val first = callJson(LIST, "controllerFilter" to WEB_PACKAGE, "compact" to true, "maxChars" to maxChars)
        assertOkPage(first)
        val revision = first["revision"].asText()

        val otherFilter = callJson(
            LIST, "controllerFilter" to "DemoController", "compact" to true,
            "maxChars" to maxChars, "offset" to 1, "expectedRevision" to revision
        )
        val otherProjection = callJson(
            LIST, "controllerFilter" to WEB_PACKAGE, "compact" to false,
            "maxChars" to maxChars, "offset" to 1, "expectedRevision" to revision
        )

        assertResultChanged("another controllerFilter", otherFilter)
        assertResultChanged("another projection", otherProjection)
    }

    fun testContractsArePagedOneAtATimeWhenTwoDoNotFit() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(CONTRACT, "urlPattern" to ROUTES_URL)
        val contracts = unbounded["endpoints"].toList()
        assertEquals("Precondition: two contracts match ${ROUTES_URL}", 2, contracts.size)
        val maxChars = budgetForOneRecord(unbounded)
        assertTrue(
            "Precondition: both contracts together must exceed $maxChars",
            envelopeLength(unbounded) + contracts.sumOf { encodedLength(it) } + 1 > maxChars
        )

        val pages = followPages(maxChars) { offset, revision ->
            listOfNotNull(
                "urlPattern" to ROUTES_URL,
                "maxChars" to maxChars,
                "offset" to offset,
                revision?.let { "expectedRevision" to it },
            ).let { callJson(CONTRACT, *it.toTypedArray()) }
        }

        assertEquals(listOf(1, 1), pages.map { it["endpoints"].size() })
        assertEquals(identities(unbounded["endpoints"]), pages.flatMap { identities(it["endpoints"]) })
    }

    fun testAContractLargerThanTheBudgetNamesTheBudgetThatFetchesIt() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(CONTRACT, "urlPattern" to DEMO_ITEM_URL, "httpMethod" to "GET")
        val contract = unbounded["endpoints"].single()
        assertTrue(
            "Precondition: the contract must not fit the smallest budget, it is ${encodedLength(contract)}",
            encodedLength(contract) > BoundedPageWriter.MIN_CHARS
        )

        val tooSmall = callJson(
            CONTRACT, "urlPattern" to DEMO_ITEM_URL, "httpMethod" to "GET", "maxChars" to BoundedPageWriter.MIN_CHARS
        )

        assertEquals("ERROR", tooSmall["status"]?.asText())
        assertEquals("RESPONSE_TOO_LARGE", tooSmall["error"]["code"].asText())
        val named = Regex("""\d{3,}""").find(tooSmall["error"]["message"].asText())?.value?.toInt()
        assertNotNull("The overflow must name the budget, got $tooSmall", named)

        val fetched = call(CONTRACT, "urlPattern" to DEMO_ITEM_URL, "httpMethod" to "GET", "maxChars" to named)
        assertOkPage(mapper.readTree(fetched))
        assertTrue("The named budget must fetch the contract, got ${fetched.length}", fetched.length <= named!!)
        assertEquals(identities(unbounded["endpoints"]), identities(mapper.readTree(fetched)["endpoints"]))
    }

    fun testAMissKeepsItsNeighbourhoodOnTheFirstPageWithinTheBudget() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(FIND, "urlPattern" to MISS_URL)
        val nearest = unbounded["nearestByPrefix"].toList()
        assertEquals("Precondition: the URL is a miss", 0, unbounded["totalCount"].asInt())
        assertTrue("Precondition: the whole fixture is the neighbourhood, got $nearest", nearest.size >= 3)
        assertTrue(
            "Precondition: the neighbourhood must exceed the budget, got ${encodedLength(unbounded)}",
            encodedLength(unbounded) > MISS_BUDGET
        )

        val json = call(FIND, "urlPattern" to MISS_URL, "maxChars" to MISS_BUDGET)
        val page = mapper.readTree(json)

        assertOkPage(page)
        assertTrue("The finished answer must fit $MISS_BUDGET, got ${json.length}", json.length <= MISS_BUDGET)
        val served = page["nearestByPrefix"].toList()
        assertTrue("The neighbourhood must not be dropped, got $page", served.isNotEmpty())
        assertEquals(
            "A cut neighbourhood keeps the nearest routes first",
            identities(nearest.take(served.size)), identities(served)
        )
        assertEquals(
            "A cut neighbourhood says so",
            served.size < nearest.size, page["nearestByPrefixTruncated"]?.asBoolean()
        )
        assertEquals(unbounded["sharedPrefix"].asText(), page["sharedPrefix"].asText())
    }

    /**
     * A miss serves no endpoint, so its page ends with the terminal continuation - `truncated` false and `nextOffset`
     * null - and every budget just below a page holding k neighbours must either fit fewer of them or refuse.
     * The budgets are the exact lengths of those pages less 1 to 4 characters, the window in which a page measured
     * with a non-terminal continuation would be sent over its budget.
     */
    fun testAMissPageNeverExceedsItsBudget() = runBlockingReleasingWriteIntent<Unit> {
        val unbounded = callJson(FIND, "urlPattern" to MISS_URL, "maxChars" to BoundedPageWriter.MAX_CHARS) as ObjectNode
        assertOkPage(unbounded)
        assertEquals("Precondition: the URL is a miss", 0, unbounded["totalCount"].asInt())
        assertFalse("Precondition: the unbounded miss is the terminal page", unbounded["truncated"].asBoolean())
        assertTrue("Precondition: the unbounded miss is the terminal page", unbounded["nextOffset"].isNull)
        assertFalse(
            "Precondition: the unbounded miss carries its whole neighbourhood",
            unbounded.has(NEAREST_TRUNCATED)
        )
        val nearest = unbounded["nearestByPrefix"].toList()
        assertTrue("Precondition: the miss has at least three neighbours, got $nearest", nearest.size >= 3)

        val budgets = (1..nearest.size).flatMap { k ->
            val pageLength = encodedLength(missPageWith(unbounded, nearest, k))
            (pageLength - 4..pageLength - 1).map { k to it }
        }.filter { (_, maxChars) -> maxChars in BoundedPageWriter.MIN_CHARS..BoundedPageWriter.MAX_CHARS }
        assertTrue("Precondition: some page length must leave a valid budget 1-4 characters below it", budgets.isNotEmpty())

        val overBudget = budgets.mapNotNull { (k, maxChars) ->
            val json = call(FIND, "urlPattern" to MISS_URL, "maxChars" to maxChars)
            val status = mapper.readTree(json)["status"]?.asText()
            val error = mapper.readTree(json)["error"]?.get("code")?.asText()
            assertTrue(
                "maxChars $maxChars must answer OK or RESPONSE_TOO_LARGE, got $json",
                status == "OK" || error == "RESPONSE_TOO_LARGE"
            )
            "k=$k maxChars=$maxChars length=${json.length}".takeIf { status == "OK" && json.length > maxChars }
        }
        assertTrue("A miss page must never exceed its budget, got $overBudget", overBudget.isEmpty())
    }

    private fun missPageWith(unbounded: ObjectNode, nearest: List<JsonNode>, k: Int): ObjectNode =
        unbounded.deepCopy().apply {
            putArray("nearestByPrefix").addAll(nearest.take(k))
            if (k < nearest.size) put(NEAREST_TRUNCATED, true)
        }

    fun testFindWithoutABudgetAnswersAsBefore() = runBlockingReleasingWriteIntent<Unit> {
        val page = callJson(FIND, "urlPattern" to DEMO_URL)

        assertFalse(page["truncated"].asBoolean())
        assertEquals(TODAY_FIND_DEMO, identities(page["endpoints"]))
    }

    /** Today the listing follows the searcher's order across controllers, which differs between runs. */
    fun testListingWithoutABudgetAnswersAsBefore() = runBlockingReleasingWriteIntent<Unit> {
        val page = callJson(LIST, "controllerFilter" to WEB_PACKAGE)

        assertFalse(page["truncated"].asBoolean())
        assertEquals(TODAY_LIST_WEB.sorted(), identities(page["endpoints"]).sorted())
    }

    fun testContractWithoutABudgetAnswersAsBefore() = runBlockingReleasingWriteIntent<Unit> {
        val page = callJson(CONTRACT, "urlPattern" to ROUTES_URL)

        assertFalse(page["truncated"].asBoolean())
        assertEquals(TODAY_CONTRACT_ROUTES, identities(page["endpoints"]))
    }

    /**
     * A budget for the largest record of [unbounded] in an envelope the size of today's one plus the paging keys,
     * so a page holds at least one record and, when the records are larger than [PAGING_KEYS_ALLOWANCE], not all.
     */
    private fun budgetForOneRecord(unbounded: JsonNode): Int {
        val records = unbounded["endpoints"].toList()
        assertTrue("Precondition: the answer must hold at least two records, got ${records.size}", records.size >= 2)
        assertTrue(
            "Precondition: every record must be larger than the paging keys allowance",
            records.all { encodedLength(it) > PAGING_KEYS_ALLOWANCE }
        )
        val maxChars = envelopeLength(unbounded) + PAGING_KEYS_ALLOWANCE + records.maxOf { encodedLength(it) }
        assertTrue(
            "Precondition: $maxChars must be a valid budget",
            maxChars in BoundedPageWriter.MIN_CHARS..BoundedPageWriter.MAX_CHARS
        )
        assertTrue(
            "Precondition: the unbounded answer (${encodedLength(unbounded)}) must exceed $maxChars",
            encodedLength(unbounded) > maxChars
        )
        return maxChars
    }

    private suspend fun followPages(
        maxChars: Int,
        pageAt: suspend (offset: Int, revision: String?) -> JsonNode,
    ): List<JsonNode> {
        val pages = mutableListOf<JsonNode>()
        var offset = 0
        var revision: String? = null
        while (true) {
            val page = pageAt(offset, revision)
            assertOkPage(page)
            assertTrue("Page at $offset exceeds $maxChars", encodedLength(page) <= maxChars)
            pages.add(page)
            val next = page["nextOffset"]
            assertNotNull("A page must carry nextOffset, null when it is the last one, got $page", next)
            if (next.isNull) return pages
            assertTrue("A continuation must advance past $offset, got $next", next.asInt() > offset)
            assertTrue("Paging did not terminate", pages.size <= MAX_PAGES)
            offset = next.asInt()
            revision = page["revision"].asText()
        }
    }

    private fun assertOkPage(page: JsonNode) {
        assertEquals("A bounded page carries its status, got $page", "OK", page["status"]?.asText())
        assertTrue("A bounded page carries its revision, got $page", page["revision"]?.asText().orEmpty().isNotEmpty())
        assertNotNull("A bounded page carries its offset, got $page", page["offset"])
    }

    private fun assertResultChanged(what: String, page: JsonNode) {
        assertEquals("A revision of $what must be rejected, got $page", "ERROR", page["status"]?.asText())
        assertEquals("RESULT_CHANGED", page["error"]["code"].asText())
    }

    private fun identities(nodes: Iterable<JsonNode>): List<String> = nodes.map {
        "${it["httpMethods"]} ${it["fullPath"].asText()} ${it["methodName"].asText()}"
    }

    private fun encodedLength(node: JsonNode): Int = mapper.writeValueAsString(node).length

    private fun envelopeLength(answer: JsonNode): Int =
        encodedLength(answer) - encodedLength(answer["endpoints"]) + "[]".length

    private suspend fun callJson(toolName: String, vararg arguments: Pair<String, Any?>): JsonNode =
        mapper.readTree(call(toolName, *arguments))

    private suspend fun call(toolName: String, vararg arguments: Pair<String, Any?>): String {
        val tool = SpringBootApplicationMcpToolset::class.functions.single {
            it.findAnnotation<McpTool>()?.name == toolName
        }
        val declared = tool.parameters.associateBy { it.name }
        val named = (arguments.toList() + ("projectPath" to project.basePath)).toMap()
        val undeclared = named.keys - declared.keys
        assertTrue("$toolName declares no parameter $undeclared", undeclared.isEmpty())
        val values = mutableMapOf<KParameter, Any?>(tool.instanceParameter!! to toolset)
        named.forEach { (name, value) -> values[declared.getValue(name)] = value }
        return tool.callSuspendBy(values) as String
    }

    private companion object {
        const val FIND = "explyt_find_spring_endpoint"
        const val LIST = "explyt_get_spring_http_endpoints"
        const val CONTRACT = "explyt_get_spring_endpoint_contract"

        const val DEMO_URL = "/api/demo"
        const val DEMO_ITEM_URL = "/api/demo/items/{id}"
        const val ROUTES_URL = "/api/routes"
        const val MISS_URL = "/api/unknown"
        const val WEB_PACKAGE = "com.example.app.web"

        const val PAGING_KEYS_ALLOWANCE = 200
        const val MISS_BUDGET = 700
        const val NEAREST_TRUNCATED = "nearestByPrefixTruncated"
        const val MAX_PAGES = 50

        val TODAY_FIND_DEMO = listOf(
            """["GET"] /api/demo/trimmed trimmed""",
            """["POST"] /api/demo/items createItem""",
            """["GET"] /api/demo/manually-created/{id} manuallyCreated""",
            """["GET"] /api/demo/wrapped/{id} wrapped""",
            """["GET"] /api/demo/items/{id} getItem""",
        )
        val TODAY_LIST_WEB = listOf(
            """["GET"] /api/resolver/items/{id} getItem""",
            """["GET"] /api/demo/items/{id} getItem""",
            """["POST"] /api/demo/items createItem""",
            """["GET"] /api/demo/wrapped/{id} wrapped""",
            """["GET"] /api/demo/trimmed trimmed""",
            """["GET"] /api/demo/manually-created/{id} manuallyCreated""",
            """["GET"] /api/routes/{id} byId""",
            """["GET"] /api/routes/export export""",
        )
        val TODAY_CONTRACT_ROUTES = listOf(
            """["GET"] /api/routes/export export""",
            """["GET"] /api/routes/{id} byId""",
        )
    }
}
