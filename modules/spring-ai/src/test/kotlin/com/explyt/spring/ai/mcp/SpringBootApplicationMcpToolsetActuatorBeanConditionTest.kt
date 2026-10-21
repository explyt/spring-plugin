/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.runconfiguration.SpringToolRunConfigurationsSettingsState
import com.explyt.spring.core.service.ProfileActivation
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.beans.BeanConditionRecord
import com.explyt.spring.core.service.conditional.ConditionReason
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.runBlockingReleasingWriteIntent
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.testFramework.IndexingTestUtil

class SpringBootApplicationMcpToolsetActuatorBeanConditionTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootActuatorAutoConfigure_4_1_0,
        TestLibrary.springBootHealth_4_1_0,
        TestLibrary.springWebMvc_6_0_7,
    )

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/app/DemoApplication.kt", """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class DemoApplication
            """.trimIndent()
        )
    }

    fun testPropertyGatedEndpointWithoutPropertyIsInactive() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Inactive>(PS_ETL)

        val psEtl = exactMatch(PS_ETL_PATH)

        assertEquals("EXPOSED", psEtl["exposed"]?.asText())
        assertEquals("UNRESTRICTED", psEtl["access"]?.asText())
        val condition = beanConditionOf(psEtl)
        assertEquals("INACTIVE", condition["state"]?.asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"]?.asText())
        assertEquals(PS_ETL, condition["carrier"]?.asText())
        assertTrue("detail names the gating key, got $condition", condition["detail"]?.asText().orEmpty().contains(GATING_KEY))
        assertFalse("reasons belong to UNDECIDED only, got $condition", condition.has("reasons"))
    }

    fun testProfileInactiveEndpointNamesTheProfileAsItsBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL, "spring.profiles.active=prod")
        addDevOnlyEndpoint()
        assertEquals(
            "precondition: $DEV_ONLY is inactive by its profile",
            ProfileActivation.INACTIVE,
            SpringSearchService.getInstance(project).profileActivation(myFixture.findClass(DEV_ONLY))
        )
        assertEndpointModelSays<ConditionVerdict.Inactive>(DEV_ONLY)

        val devOnly = exactMatch(DEV_ONLY_PATH)

        assertEquals("EXPOSED", devOnly["exposed"]?.asText())
        val condition = beanConditionOf(devOnly)
        assertEquals("INACTIVE", condition["state"]?.asText())
        assertEquals(PROFILE, condition["annotation"]?.asText())
        assertEquals(DEV_ONLY, condition["carrier"]?.asText())
        assertTrue("detail names the profile, got $condition", condition["detail"]?.asText().orEmpty().contains("'dev'"))
        assertFalse("reasons belong to UNDECIDED only, got $condition", condition.has("reasons"))
    }

    fun testProfileActiveEndpointHasNoBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL, "spring.profiles.active=dev")
        addDevOnlyEndpoint()
        assertEndpointModelSays<ConditionVerdict.Active>(DEV_ONLY)

        val devOnly = exactMatch(DEV_ONLY_PATH)

        assertFalse("an active endpoint carries no beanCondition, got $devOnly", devOnly.has("beanCondition"))
    }

    private fun addDevOnlyEndpoint() {
        myFixture.addFileToProject(
            "com/app/DevOnlyEndpoint.kt", """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.context.annotation.Profile
            import org.springframework.stereotype.Component

            @Component
            @Endpoint(id = "devOnly")
            @Profile("dev")
            class DevOnlyEndpoint {
                @ReadOperation
                fun status() = "idle"
            }
            """.trimIndent()
        )
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testPropertyGatedEndpointWithMatchingPropertyHasNoBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL, "$GATING_KEY=true")
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Active>(PS_ETL)

        val psEtl = exactMatch(PS_ETL_PATH)

        assertEquals("EXPOSED", psEtl["exposed"]?.asText())
        assertFalse("an active endpoint carries no beanCondition, got $psEtl", psEtl.has("beanCondition"))
    }

    fun testUnresolvablePlaceholderMakesTheEndpointUndecided() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL, "$GATING_KEY=\${CH_ENABLED}")
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Undecided>(PS_ETL)

        val condition = beanConditionOf(exactMatch(PS_ETL_PATH))

        assertEquals("UNDECIDED", condition["state"]?.asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"]?.asText())
        assertEquals(PS_ETL, condition["carrier"]?.asText())
        assertEquals(
            listOf(ConditionReason.PROPERTY_UNRESOLVABLE.name),
            condition["reasons"]?.map { it.asText() }
        )
    }

    fun testEndpointRegisteredByGatedConfigurationNamesTheConfiguration() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addEndpointRegisteredByGatedConfiguration()
        assertEndpointModelSays<ConditionVerdict.Inactive>(CH_ETL)

        val condition = beanConditionOf(exactMatch(CH_ETL_PATH))

        assertEquals("INACTIVE", condition["state"]?.asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"]?.asText())
        assertEquals(CLICKHOUSE_CONFIG, condition["carrier"]?.asText())
    }

    fun testBuiltInHealthHasNoBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEquals(listOf(null), endpointModelOf(HEALTH_ENDPOINT).map { it.beanCondition }.distinct())

        val health = exactMatch("/actuator/health")

        assertEquals("EXPOSED", health["exposed"]?.asText())
        assertFalse("a built-in endpoint carries no beanCondition, got $health", health.has("beanCondition"))
    }

    fun testListingKeepsTheInactiveEndpointWithItsBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Inactive>(PS_ETL)

        for (compact in listOf(true, false)) {
            val listed = listActuator(compact).filter { it["fullPath"].asText() == PS_ETL_PATH }
            assertEquals("psEtl stays listed (compact=$compact)", 1, listed.size)
            val psEtl = listed.single()
            assertEquals("EXPOSED", psEtl["exposed"]?.asText())
            val condition = beanConditionOf(psEtl)
            assertEquals("INACTIVE", condition["state"]?.asText())
            assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"]?.asText())
            assertEquals(PS_ETL, condition["carrier"]?.asText())
        }
    }

    fun testContractCarriesTheBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Inactive>(PS_ETL)

        val contract = contractOf(PS_ETL_PATH, "GET")

        assertEquals("EXPOSED", contract["exposed"]?.asText())
        val condition = beanConditionOf(contract)
        assertEquals("INACTIVE", condition["state"]?.asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"]?.asText())
        assertEquals(PS_ETL, condition["carrier"]?.asText())
        assertTrue("detail names the gating key, got $condition", condition["detail"]?.asText().orEmpty().contains(GATING_KEY))
    }

    fun testFindAnswerWithOneInactiveEndpointFitsTheClientRootBudget() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Inactive>(PS_ETL)

        val answer = toolset.findEndpoint(urlPattern = PS_ETL_PATH, projectPath = projectPath())

        assertEquals("INACTIVE", beanConditionOf(mapper.readTree(answer)["endpoints"].single())["state"]?.asText())
        assertTrue("answer of ${answer.length} chars exceeds $CLIENT_ROOT_BUDGET: $answer", answer.length <= CLIENT_ROOT_BUDGET)
    }

    fun testDisabledConditionEvaluationLeavesNoBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val wasEnabled = settings.isBeanFilterEnabled
        settings.isBeanFilterEnabled = false
        try {
            addProperties(EXPOSE_ALL)
            addPsEtlEndpoint()
            assertEquals(
                "precondition: without condition evaluation the model holds no verdict",
                listOf(null),
                endpointModelOf(PS_ETL).map { it.beanCondition }.distinct()
            )

            val psEtl = exactMatch(PS_ETL_PATH)

            assertEquals("EXPOSED", psEtl["exposed"]?.asText())
            assertEquals("UNRESTRICTED", psEtl["access"]?.asText())
            assertFalse("no verdict, no beanCondition, got $psEtl", psEtl.has("beanCondition"))
            assertEquals(1, listActuator(compact = true).count { it["fullPath"].asText() == PS_ETL_PATH })
        } finally {
            settings.isBeanFilterEnabled = wasEnabled
        }
    }

    fun testFindFirstPageNamesConditionsNotEvaluatedWhenEvaluationIsDisabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val page = withConditionEvaluation(enabled = false) { findPage() }

        assertServedPage(page)
        assertConditionsNotEvaluated(page)
    }

    fun testFindContinuationPageNamesConditionsNotEvaluatedWhenEvaluationIsDisabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val continuation = withConditionEvaluation(enabled = false) {
            val first = findPage()
            assertServedPage(first)
            findPage(offset = nextOffsetOf(first), expectedRevision = first["revision"].asText())
        }

        assertServedPage(continuation)
        assertConditionsNotEvaluated(continuation)
    }

    fun testFindPageHasNoLimitationsWhenEvaluationIsEnabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val page = withConditionEvaluation(enabled = true) { findPage() }

        assertServedPage(page)
        assertFalse("evaluated conditions leave no limitation, got $page", page.has(FIELD_LIMITATIONS))
    }

    fun testListingFirstPageNamesConditionsNotEvaluatedWhenEvaluationIsDisabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val first = withConditionEvaluation(enabled = false) { actuatorListingPages() }.first()

        assertServedPage(first)
        assertConditionsNotEvaluated(first)
    }

    fun testListingContinuationPagesNameConditionsNotEvaluatedWhenEvaluationIsDisabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val pages = withConditionEvaluation(enabled = false) { actuatorListingPages() }

        assertTrue("precondition: the listing spans more than one page, got ${pages.size}", pages.size >= 2)
        for (continuation in pages.drop(1)) {
            assertServedPage(continuation)
            assertConditionsNotEvaluated(continuation)
        }
    }

    fun testListingPagesHaveNoLimitationsWhenEvaluationIsEnabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val pages = withConditionEvaluation(enabled = true) { actuatorListingPages() }

        assertTrue("precondition: the listing spans more than one page, got ${pages.size}", pages.size >= 2)
        for (page in pages) {
            assertServedPage(page)
            assertFalse("evaluated conditions leave no limitation, got $page", page.has(FIELD_LIMITATIONS))
        }
    }

    fun testContractFirstPageNamesConditionsNotEvaluatedWhenEvaluationIsDisabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val page = withConditionEvaluation(enabled = false) { contractPage() }

        assertServedPage(page)
        assertConditionsNotEvaluated(page)
    }

    fun testContractContinuationPageNamesConditionsNotEvaluatedWhenEvaluationIsDisabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val continuation = withConditionEvaluation(enabled = false) {
            val first = contractPage()
            assertServedPage(first)
            contractPage(offset = nextOffsetOf(first), expectedRevision = first["revision"].asText())
        }

        assertServedPage(continuation)
        assertConditionsNotEvaluated(continuation)
    }

    fun testContractPageHasNoLimitationsWhenEvaluationIsEnabled() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()

        val page = withConditionEvaluation(enabled = true) { contractPage() }

        assertServedPage(page)
        assertFalse("evaluated conditions leave no limitation, got $page", page.has(FIELD_LIMITATIONS))
    }

    private inline fun <T> withConditionEvaluation(enabled: Boolean, block: () -> T): T {
        val settings = SpringToolRunConfigurationsSettingsState.getInstance()
        val wasEnabled = settings.isBeanFilterEnabled
        settings.isBeanFilterEnabled = enabled
        try {
            return block()
        } finally {
            settings.isBeanFilterEnabled = wasEnabled
        }
    }

    private suspend fun findPage(offset: Int = 0, expectedRevision: String? = null): JsonNode =
        mapper.readTree(
            toolset.findEndpoint(
                urlPattern = ACTUATOR_PREFIX,
                projectPath = projectPath(),
                offset = offset,
                maxChars = FIND_PAGE_CHARS,
                expectedRevision = expectedRevision,
            )
        )

    private suspend fun contractPage(offset: Int = 0, expectedRevision: String? = null): JsonNode =
        mapper.readTree(
            toolset.getEndpointContract(
                urlPattern = ACTUATOR_PREFIX,
                projectPath = projectPath(),
                offset = offset,
                maxChars = CONTRACT_PAGE_CHARS,
                expectedRevision = expectedRevision,
            )
        )

    private suspend fun actuatorListingPages(): List<JsonNode> =
        toolset.endpointListingPages(
            projectPath(),
            endpointType = "ACTUATOR",
            compact = true,
            maxChars = LISTING_PAGE_CHARS,
            mapper = mapper,
        )

    private fun assertServedPage(page: JsonNode) {
        assertEquals("precondition: a page is served, got $page", "OK", page["status"]?.asText())
        assertTrue("precondition: the page holds endpoints, got $page", (page["endpoints"]?.size() ?: 0) > 0)
    }

    private fun nextOffsetOf(page: JsonNode): Int {
        assertTrue("precondition: the first page is cut, got $page", page["truncated"]?.asBoolean() == true)
        val next = page["nextOffset"]
        assertTrue("precondition: the first page names its continuation, got $page", next != null && next.isInt)
        return next.asInt()
    }

    private fun assertConditionsNotEvaluated(page: JsonNode) {
        assertEquals(
            "a page answered without condition evaluation names that limitation, got $page",
            listOf(BeanConditionRecord.CONDITIONS_NOT_EVALUATED),
            page[FIELD_LIMITATIONS]?.map { it.asText() }
        )
    }

    fun testNearestRouteOfAMissCarriesTheBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Inactive>(PS_ETL)

        val root = mapper.readTree(toolset.findEndpoint(urlPattern = "$PS_ETL_PATH/zzqqmissing", projectPath = projectPath()))

        assertEquals("precondition: the URL matches nothing, got $root", 0, root["endpoints"].size())
        assertEquals("precondition: the neighbourhood is psEtl's prefix", PS_ETL_PATH, root["sharedPrefix"]?.asText())
        val psEtl = root["nearestByPrefix"].single { it["fullPath"].asText() == PS_ETL_PATH }
        assertEquals("INACTIVE", beanConditionOf(psEtl)["state"]?.asText())
        assertEquals(PS_ETL, beanConditionOf(psEtl)["carrier"]?.asText())
    }

    fun testEndpointWithAnActiveAndAnInactiveBeanHasNoBeanCondition() = runBlockingReleasingWriteIntent<Unit> {
        addProperties(EXPOSE_ALL)
        addTwiceRegisteredEndpoint()
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val verdicts = SpringSearchService.getInstance(project).conditionVerdicts(module)
            .filterKeys { it.psiClass.qualifiedName == TWICE }
            .values
        assertEquals("precondition: two beans of $TWICE, got $verdicts", 2, verdicts.size)
        assertEquals(1, verdicts.count { it is ConditionVerdict.Active })
        assertEquals(1, verdicts.count { it is ConditionVerdict.Inactive })
        assertEndpointModelSays<ConditionVerdict.Active>(TWICE)

        val listed = listActuator(compact = true).filter { it["fullPath"].asText() == TWICE_PATH }
        val found = exactMatch(TWICE_PATH)

        assertEquals(1, listed.size)
        assertFalse("the most active bean decides, got ${listed.single()}", listed.single().has("beanCondition"))
        assertFalse("the most active bean decides, got $found", found.has("beanCondition"))
    }

    private fun addTwiceRegisteredEndpoint() {
        myFixture.addFileToProject(
            "com/app/TwiceEndpoint.kt", """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.stereotype.Component

            @Component
            @Endpoint(id = "twice")
            class TwiceEndpoint {
                @ReadOperation
                fun status() = "idle"
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/TwiceEndpointConfig.kt", """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["clickhouse.enabled"], havingValue = "true")
            class TwiceEndpointConfig {
                @Bean
                fun secondTwiceEndpoint() = TwiceEndpoint()
            }
            """.trimIndent()
        )
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun addProperties(vararg lines: String) {
        myFixture.addFileToProject("application.properties", lines.joinToString("\n"))
    }

    private fun addPsEtlEndpoint() {
        myFixture.addFileToProject(
            "com/app/PsEtlEndpoint.kt", """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.stereotype.Component

            @Component
            @Endpoint(id = "psEtl")
            @ConditionalOnProperty(name = ["clickhouse.enabled"], havingValue = "true")
            class PsEtlEndpoint {
                @ReadOperation
                fun status() = "idle"
            }
            """.trimIndent()
        )
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun addEndpointRegisteredByGatedConfiguration() {
        myFixture.addFileToProject(
            "com/app/ChEtlEndpoint.kt", """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "chEtl")
            class ChEtlEndpoint {
                @ReadOperation
                fun status() = "idle"
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/ClickhouseEndpointConfig.kt", """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["clickhouse.enabled"], havingValue = "true")
            class ClickhouseEndpointConfig {
                @Bean
                fun chEtlEndpoint() = ChEtlEndpoint()
            }
            """.trimIndent()
        )
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    private fun endpointModelOf(className: String) = run {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        SpringWebEndpointsLoader.EP_NAME.getExtensions(project).asSequence()
            .filter { it.getType() == EndpointType.ACTUATOR }
            .filter { it.isApplicable(module) }
            .flatMap { it.searchEndpoints(module) }
            .filter { it.containingClass?.qualifiedName == className }
            .toList()
            .also { assertFalse("precondition: $className is in the endpoint model", it.isEmpty()) }
    }

    private inline fun <reified T : ConditionVerdict> assertEndpointModelSays(className: String) {
        val verdicts = endpointModelOf(className).map { it.beanCondition }.distinct()
        assertInstanceOf(verdicts.single(), T::class.java)
    }

    private fun beanConditionOf(record: JsonNode): JsonNode {
        val condition = record["beanCondition"]
        assertNotNull("the record carries a beanCondition, got $record", condition)
        return condition!!
    }

    private fun projectPath(): String = project.basePath ?: ""

    private suspend fun exactMatch(url: String): JsonNode =
        mapper.readTree(toolset.findEndpoint(urlPattern = url, projectPath = projectPath()))["endpoints"]
            .single { it["fullPath"].asText() == url }

    private suspend fun contractOf(url: String, method: String): JsonNode =
        mapper.readTree(
            toolset.getEndpointContract(urlPattern = url, projectPath = projectPath(), httpMethod = method)
        )["endpoints"].single { it["fullPath"].asText() == url }

    private suspend fun listActuator(compact: Boolean): List<JsonNode> =
        toolset.allListedEndpoints(projectPath(), endpointType = "ACTUATOR", compact = compact, mapper = mapper)

    private companion object {
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val GATING_KEY = "clickhouse.enabled"
        const val PROFILE = "org.springframework.context.annotation.Profile"
        const val DEV_ONLY = "com.app.DevOnlyEndpoint"
        const val DEV_ONLY_PATH = "/actuator/devOnly"
        const val EXPOSE_ALL = "management.endpoints.web.exposure.include=*"
        const val PS_ETL = "com.app.PsEtlEndpoint"
        const val PS_ETL_PATH = "/actuator/psEtl"
        const val CH_ETL = "com.app.ChEtlEndpoint"
        const val CH_ETL_PATH = "/actuator/chEtl"
        const val CLICKHOUSE_CONFIG = "com.app.ClickhouseEndpointConfig"
        const val HEALTH_ENDPOINT = "org.springframework.boot.health.actuate.endpoint.HealthEndpoint"
        const val CLIENT_ROOT_BUDGET = 2000
        const val TWICE = "com.app.TwiceEndpoint"
        const val TWICE_PATH = "/actuator/twice"
        const val ACTUATOR_PREFIX = "/actuator"
        const val FIELD_LIMITATIONS = "limitations"
        const val FIND_PAGE_CHARS = 4000
        const val CONTRACT_PAGE_CHARS = 8000
        const val LISTING_PAGE_CHARS = 1500
    }
}
