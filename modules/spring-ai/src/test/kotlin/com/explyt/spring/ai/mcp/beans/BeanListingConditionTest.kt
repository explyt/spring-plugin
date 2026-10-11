/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.ai.mcp.BoundedPageWriter
import com.explyt.spring.ai.mcp.SpringBootApplicationMcpToolset
import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.conditional.ConditionReason
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.mcpserver.McpToolset
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.testFramework.IndexingTestUtil
import kotlinx.coroutines.runBlocking

class BeanListingConditionTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    private val listingToolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()
    private val nativeFixture by lazy { NativeBeanListingFixture(project) }

    override fun setUp() {
        super.setUp()
        addJava(
            "App",
            """
            package com.explyt.demo;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class App {}
            """
        )
    }

    override fun tearDown() {
        try {
            nativeFixture.clear()
        } finally {
            super.tearDown()
        }
    }

    fun testControllerExcludedByItsConditionIsListedLastAsInactive() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val page = listing(limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS)

        val beans = completeRows(page)
        val inactive = beans.singleOrNull { it["beanName"].asText() == SYNC_ADMIN }
        assertNotNull("the excluded bean is listed, not hidden, got $page", inactive)
        val firstInactive = beans.indexOfFirst { stateOf(it) == INACTIVE }
        val active = beans.take(firstInactive)
        val excluded = beans.drop(firstInactive)
        assertTrue("Precondition: the fixture has active controllers, got $beans", active.any { it["beanName"].asText() == ALPHA })
        assertTrue("every inactive row is ranked after every active one, got $beans", excluded.all { stateOf(it) == INACTIVE })
        assertEquals("active rows keep the origin/name order", active.sortedWith(LISTING_ORDER).map(::rowKey), active.map(::rowKey))
        assertEquals("inactive rows keep the origin/name order", excluded.sortedWith(LISTING_ORDER).map(::rowKey), excluded.map(::rowKey))
        assertEquals(listOf(ALPHA, ZULU, SYNC_ADMIN), projectBeanNames(beans))
        assertFalse("evaluated conditions add no envelope limitation, got $page", page.has("limitations"))

        val condition = conditionOf(inactive!!)
        assertEquals(setOf("state", "annotation", "carrier", "detail"), keysOf(condition))
        assertEquals(INACTIVE, condition["state"].asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"].asText())
        assertEquals(SYNC_ADMIN_CONTROLLER, condition["carrier"].asText())
        assertTrue("detail must name the property, got $condition", "admin.sync.enabled" in condition["detail"].asText())
        assertEquals(
            "the listing reports the condition exactly as the lookup tool does",
            lookupInactiveConditionOf(SYNC_ADMIN), condition
        )
    }

    fun testControllerWhoseConditionHoldsIsAnOrdinaryRow() = runBlocking<Unit> {
        addProperties("admin.sync.enabled=true")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Active>(SYNC_ADMIN_CONTROLLER)

        val page = listing(limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS)

        val beans = completeRows(page)
        val sync = beans.single { it["beanName"].asText() == SYNC_ADMIN }
        assertFalse("an active row carries no condition, got $sync", sync.has("condition"))
        assertEquals(listOf(ALPHA, SYNC_ADMIN, ZULU), projectBeanNames(beans))
        assertTrue("no project row is conditioned, got $beans", projectRows(beans).none { it.has("condition") })
    }

    fun testUnresolvablePlaceholderMakesTheRowUndecidedInItsOrdinaryPlace() = runBlocking<Unit> {
        addProperties("admin.sync.enabled=\${SYNC_ENABLED}")
        addActiveControllers()
        addSyncAdminController()
        assertUndecidedBecause(SYNC_ADMIN_CONTROLLER, ConditionReason.PROPERTY_UNRESOLVABLE)

        val page = listing(limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS)

        val beans = completeRows(page)
        assertEquals("an undecided bean keeps its ordinary place", listOf(ALPHA, SYNC_ADMIN, ZULU), projectBeanNames(beans))
        val sync = beans.single { it["beanName"].asText() == SYNC_ADMIN }
        val condition = conditionOf(sync)
        assertEquals(setOf("state", "reasons", "annotation", "carrier", "detail"), keysOf(condition))
        assertEquals("UNDECIDED", condition["state"].asText())
        assertEquals(listOf("PROPERTY_UNRESOLVABLE"), condition["reasons"].map { it.asText() })
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"].asText())
        assertEquals(SYNC_ADMIN_CONTROLLER, condition["carrier"].asText())
        assertTrue("detail must name the property, got $condition", "admin.sync.enabled" in condition["detail"].asText())
        assertEquals(
            "the listing reports the condition exactly as the lookup tool does",
            lookupActiveConditionOf(SYNC_ADMIN), condition
        )
    }

    fun testUnconditionedServiceRowIsUnchanged() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addJava(
            "PlainService",
            """
            package com.explyt.demo;

            import org.springframework.stereotype.Service;

            @Service
            public class PlainService {}
            """
        )

        val page = listing(beanType = COMPONENT, origin = PROJECT)

        val row = completeRows(page).single()
        assertEquals(listOf("beanName", "className", "moduleName", "source", "origin"), row.fieldNames().asSequence().toList())
        assertEquals(
            """{"beanName":"plainService","className":"com.explyt.demo.PlainService",""" +
                    """"moduleName":"${module.name}","source":"STATIC","origin":"PROJECT"}""",
            mapper.writeValueAsString(row)
        )
    }

    fun testProjectOriginFilterKeepsTheInactiveProjectRow() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)

        val page = listing(origin = PROJECT)

        val beans = completeRows(page)
        assertEquals("the inactive project row is counted, got $page", 3, page["totalCount"].asInt())
        assertEquals(listOf(ALPHA, ZULU, SYNC_ADMIN), beans.map { it["beanName"].asText() })
        assertEquals(INACTIVE, stateOf(beans.last()))
        assertTrue(beans.all { it["origin"].asText() == PROJECT })
    }

    fun testLibraryOriginFilterLeavesTheInactiveProjectRowOut() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)
        val unfiltered = completeRows(listing(limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS))
        assertTrue(
            "Precondition: without a filter the inactive row is listed, got $unfiltered",
            unfiltered.any { it["beanName"].asText() == SYNC_ADMIN && stateOf(it) == INACTIVE }
        )

        val page = listing(origin = "LIBRARY", limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS)

        val beans = completeRows(page)
        assertTrue("a LIBRARY page holds no project row, got $page", beans.none { it["beanName"].asText() == SYNC_ADMIN })
        assertTrue(beans.all { it["origin"].asText() == "LIBRARY" })
        assertEquals(beans.size, page["totalCount"].asInt())
    }

    fun testInactiveRowIsReachedThroughTheContinuation() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)
        val first = listing(origin = PROJECT, limit = 2)

        assertEquals("totalCount counts the inactive row, got $first", 3, first["totalCount"].asInt())
        assertEquals(listOf(ALPHA, ZULU), rows(first).map { it["beanName"].asText() })
        assertTrue("page 1 holds only active rows, got $first", rows(first).none { stateOf(it) == INACTIVE })
        assertTrue("the first page is truncated before the inactive row, got $first", first["truncated"].asBoolean())
        assertEquals(2, first["nextOffset"].asInt())

        val second = listing(
            origin = PROJECT,
            limit = 2,
            offset = first["nextOffset"].asInt(),
            expectedRevision = first["revision"].asText()
        )

        val last = rows(second).single()
        assertEquals(SYNC_ADMIN, last["beanName"].asText())
        assertEquals(INACTIVE, stateOf(last))
        assertFalse(second["truncated"].asBoolean())
        assertTrue("Terminal nextOffset is present as JSON null: $second", second["nextOffset"]?.isNull == true)
    }

    fun testTogglingTheConditionPropertyChangesTheRevision() = runBlocking<Unit> {
        val properties = addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)
        val before = listing(limit = 1)
        assertTrue("Precondition: the first page is truncated, got $before", before["truncated"].asBoolean())

        replaceText(properties, "admin.sync.enabled=true")

        val after = listing(limit = 1)
        val stale = listing(limit = 1, offset = 1, expectedRevision = before["revision"].asText())
        assertVerdict<ConditionVerdict.Active>(SYNC_ADMIN_CONTROLLER)
        assertFalse(
            "a flipped condition is another answer and must carry another revision",
            before["revision"].asText() == after["revision"].asText()
        )
        assertEquals("ERROR", stale["status"].asText())
        assertEquals("RESULT_CHANGED", stale["error"]["code"].asText())
    }

    fun testDefaultPageWithThreeInactiveRowsFitsTheClientBudget() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        INACTIVE_CONTROLLERS.forEach { name ->
            addJava(
                name,
                """
                package com.explyt.demo;

                import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
                import org.springframework.stereotype.Controller;

                @Controller
                @ConditionalOnProperty(name = "${name.lowercase()}.enabled", havingValue = "true")
                public class $name {}
                """
            )
        }
        INACTIVE_CONTROLLERS.forEach { assertVerdict<ConditionVerdict.Inactive>("com.explyt.demo.$it") }

        val json = listingToolset.applicationBeans(
            applicationClassName = APPLICATION,
            projectPath = project.basePath,
            beanType = CONTROLLER,
            source = "STATIC",
            origin = PROJECT,
        )
        val page = mapper.readTree(json)

        assertEquals("answer was $json", "OK", page["status"].asText())
        assertTrue("payload was ${json.length} chars", json.length <= DEFAULT_MAX_CHARS)
        assertTrue("MCP-wrapped payload was ${wrappedLength(json)} chars", wrappedLength(json) <= MAX_CLIENT_PAYLOAD)
        val beans = rows(page)
        assertEquals("all three inactive rows are served, got $json", 3, beans.size)
        assertTrue("every row is INACTIVE, got $json", beans.all { stateOf(it) == INACTIVE })
        assertFalse(page["truncated"].asBoolean())
    }

    fun testDisabledConditionEvaluationListsNoInactiveRow() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val enabled = settings.isBeanFilterEnabled
        settings.isBeanFilterEnabled = false
        try {
            ModificationTrackerManager.getInstance(project).invalidateAll()

            val page = listing(limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS)

            val beans = completeRows(page)
            assertEquals(listOf(ALPHA, SYNC_ADMIN, ZULU), projectBeanNames(beans))
            assertTrue("no verdict without evaluation, got $beans", beans.none { it.has("condition") })
            assertEquals(listOf("CONDITIONS_NOT_EVALUATED"), page["limitations"]?.map { it.asText() })
        } finally {
            settings.isBeanFilterEnabled = enabled
            ModificationTrackerManager.getInstance(project).invalidateAll()
        }
    }

    fun testNativeRowsCarryNoStaticCondition() = runBlocking<Unit> {
        addProperties("admin.marker=present")
        addActiveControllers()
        addSyncAdminController()
        assertVerdict<ConditionVerdict.Inactive>(SYNC_ADMIN_CONTROLLER)
        nativeFixture.install(
            myFixture.findClass(APPLICATION),
            listOf(SYNC_ADMIN to SYNC_ADMIN_CONTROLLER, ALPHA to "com.explyt.demo.AlphaController")
        )

        val page = listing(source = "NATIVE", limit = BoundedPageWriter.MAX_LIMIT, maxChars = BoundedPageWriter.MAX_CHARS)

        val beans = completeRows(page)
        assertTrue("Precondition: the native snapshot answers, got $page", beans.all { it["source"].asText() == "NATIVE_SNAPSHOT" })
        assertEquals(listOf(ALPHA, SYNC_ADMIN), beans.map { it["beanName"].asText() })
        assertTrue("a recorded context passed every condition, got $beans", beans.none { it.has("condition") })
    }

    private fun addActiveControllers() {
        listOf("AlphaController", "ZuluController").forEach { name ->
            addJava(
                name,
                """
                package com.explyt.demo;

                import org.springframework.stereotype.Controller;

                @Controller
                public class $name {}
                """
            )
        }
    }

    private fun addSyncAdminController() = addJava(
        "SyncAdminController",
        """
        package com.explyt.demo;

        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
        import org.springframework.stereotype.Controller;

        @Controller
        @ConditionalOnProperty(name = "admin.sync.enabled", havingValue = "true")
        public class SyncAdminController {}
        """
    )

    private fun addJava(className: String, source: String): PsiFile {
        val file = myFixture.addFileToProject("com/explyt/demo/$className.java", source.trimIndent())
        settle()
        return file
    }

    private fun addProperties(vararg lines: String): PsiFile {
        val file = myFixture.addFileToProject("application.properties", lines.joinToString("\n"))
        settle()
        return file
    }

    private fun replaceText(file: PsiFile, text: String) {
        val documents = PsiDocumentManager.getInstance(project)
        val document = documents.getDocument(file)!!
        WriteCommandAction.runWriteCommandAction(project) {
            document.setText(text)
            documents.commitDocument(document)
        }
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun settle() {
        IndexingTestUtil.waitUntilIndexesAreReady(project)
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    private fun verdictOf(classFqn: String): ConditionVerdict? {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        return SpringSearchService.getInstance(project).conditionVerdictOf(myFixture.findClass(classFqn), module)
    }

    private inline fun <reified T : ConditionVerdict> assertVerdict(classFqn: String) {
        val verdict = verdictOf(classFqn)
        assertTrue("Precondition: $classFqn must be ${T::class.simpleName}, got $verdict", verdict is T)
    }

    private fun assertUndecidedBecause(classFqn: String, reason: ConditionReason) {
        val verdict = verdictOf(classFqn)
        assertTrue("Precondition: $classFqn must be Undecided, got $verdict", verdict is ConditionVerdict.Undecided)
        assertEquals(
            "Precondition: $classFqn undecided reason",
            listOf(reason),
            (verdict as ConditionVerdict.Undecided).conditions.map { it.reason }
        )
    }

    private suspend fun listing(
        beanType: String = CONTROLLER,
        origin: String? = null,
        offset: Int = 0,
        limit: Int = DEFAULT_LIMIT,
        maxChars: Int = DEFAULT_MAX_CHARS,
        expectedRevision: String? = null,
        source: String = "STATIC",
    ): JsonNode = mapper.readTree(
        listingToolset.applicationBeans(
            applicationClassName = APPLICATION,
            projectPath = project.basePath,
            beanType = beanType,
            source = source,
            origin = origin,
            offset = offset,
            limit = limit,
            maxChars = maxChars,
            expectedRevision = expectedRevision,
        )
    )

    private suspend fun lookup(beanName: String): JsonNode = mapper.readTree(
        lookupToolset().findSpringBean(
            projectPath = project.basePath!!,
            applicationClassName = APPLICATION,
            source = "STATIC",
            beanName = beanName
        )
    )

    private suspend fun lookupInactiveConditionOf(beanName: String): JsonNode {
        val answer = lookup(beanName)
        val inactive = answer["inactiveCandidates"]?.toList().orEmpty()
        assertEquals("Precondition: the lookup tool lists one inactive candidate, got $answer", 1, inactive.size)
        return conditionOf(inactive.single())
    }

    private suspend fun lookupActiveConditionOf(beanName: String): JsonNode {
        val answer = lookup(beanName)
        val candidates = answer["candidates"]?.toList().orEmpty()
        assertEquals("Precondition: the lookup tool lists one candidate, got $answer", 1, candidates.size)
        return conditionOf(candidates.single())
    }

    private fun lookupToolset(): SpringBeanMcpToolset {
        val toolsets = McpToolset.EP.extensionList.filterIsInstance<SpringBeanMcpToolset>()
        assertEquals("Precondition: the toolset must be registered", 1, toolsets.size)
        return toolsets.single()
    }

    private fun rows(page: JsonNode): List<JsonNode> {
        assertEquals("Expected an OK envelope, got $page", "OK", page["status"]?.asText())
        val beans = page["beans"]
        assertTrue("Expected a 'beans' array, got $page", beans?.isArray == true)
        return beans.toList()
    }

    private fun completeRows(page: JsonNode): List<JsonNode> {
        val beans = rows(page)
        assertFalse("Expected the whole listing on one page, got $page", page["truncated"].asBoolean())
        return beans
    }

    private fun projectRows(beans: List<JsonNode>): List<JsonNode> =
        beans.filter { it["origin"]?.asText() == PROJECT }

    private fun projectBeanNames(beans: List<JsonNode>): List<String> =
        projectRows(beans).map { it["beanName"].asText() }

    private fun stateOf(row: JsonNode): String? = row["condition"]?.get("state")?.asText()

    private fun conditionOf(row: JsonNode): JsonNode {
        val condition = row["condition"]
        assertNotNull("row must carry a condition, got $row", condition)
        return condition
    }

    private fun keysOf(node: JsonNode): Set<String> = node.fieldNames().asSequence().toSet()

    private fun rowKey(row: JsonNode): String = row["beanName"].asText() + "|" + row["className"].asText()

    private fun wrappedLength(json: String): Int =
        mapper.writeValueAsString(mapper.createArrayNode().add(json)).length

    private companion object {
        const val APPLICATION = "com.explyt.demo.App"
        const val SYNC_ADMIN = "syncAdminController"
        const val SYNC_ADMIN_CONTROLLER = "com.explyt.demo.SyncAdminController"
        const val ALPHA = "alphaController"
        const val ZULU = "zuluController"
        const val CONTROLLER = "CONTROLLER"
        const val COMPONENT = "COMPONENT"
        const val PROJECT = "PROJECT"
        const val INACTIVE = "INACTIVE"
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val DEFAULT_LIMIT = 8
        const val DEFAULT_MAX_CHARS = 1800
        const val MAX_CLIENT_PAYLOAD = 2000
        val INACTIVE_CONTROLLERS = listOf("BatchController", "ExportController", "ImportController")
        val ORIGIN_ORDER = listOf(PROJECT, "LIBRARY", null)
        val LISTING_ORDER: Comparator<JsonNode> = compareBy(
            { ORIGIN_ORDER.indexOf(it["origin"]?.asText()) },
            { it["beanName"].asText() },
            { it["className"].asText() }
        )
    }
}
