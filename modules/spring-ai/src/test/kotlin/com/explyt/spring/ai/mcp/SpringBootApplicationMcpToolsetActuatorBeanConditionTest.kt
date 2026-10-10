/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.core.service.conditional.ConditionReason
import com.explyt.spring.core.service.conditional.ConditionVerdict
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.loader.EndpointType
import com.explyt.spring.web.loader.SpringWebEndpointsLoader
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import com.intellij.testFramework.IndexingTestUtil
import kotlinx.coroutines.runBlocking

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

    fun testPropertyGatedEndpointWithoutPropertyIsInactive() = runBlocking<Unit> {
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

    fun testPropertyGatedEndpointWithMatchingPropertyHasNoBeanCondition() = runBlocking<Unit> {
        addProperties(EXPOSE_ALL, "$GATING_KEY=true")
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Active>(PS_ETL)

        val psEtl = exactMatch(PS_ETL_PATH)

        assertEquals("EXPOSED", psEtl["exposed"]?.asText())
        assertFalse("an active endpoint carries no beanCondition, got $psEtl", psEtl.has("beanCondition"))
    }

    fun testUnresolvablePlaceholderMakesTheEndpointUndecided() = runBlocking<Unit> {
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

    fun testEndpointRegisteredByGatedConfigurationNamesTheConfiguration() = runBlocking<Unit> {
        addProperties(EXPOSE_ALL)
        addEndpointRegisteredByGatedConfiguration()
        assertEndpointModelSays<ConditionVerdict.Inactive>(CH_ETL)

        val condition = beanConditionOf(exactMatch(CH_ETL_PATH))

        assertEquals("INACTIVE", condition["state"]?.asText())
        assertEquals(CONDITIONAL_ON_PROPERTY, condition["annotation"]?.asText())
        assertEquals(CLICKHOUSE_CONFIG, condition["carrier"]?.asText())
    }

    fun testBuiltInHealthHasNoBeanCondition() = runBlocking<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEquals(listOf(null), endpointModelOf(HEALTH_ENDPOINT).map { it.beanCondition }.distinct())

        val health = exactMatch("/actuator/health")

        assertEquals("EXPOSED", health["exposed"]?.asText())
        assertFalse("a built-in endpoint carries no beanCondition, got $health", health.has("beanCondition"))
    }

    fun testListingKeepsTheInactiveEndpointWithItsBeanCondition() = runBlocking<Unit> {
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

    fun testContractCarriesTheBeanCondition() = runBlocking<Unit> {
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

    fun testFindAnswerWithOneInactiveEndpointFitsTheClientRootBudget() = runBlocking<Unit> {
        addProperties(EXPOSE_ALL)
        addPsEtlEndpoint()
        assertEndpointModelSays<ConditionVerdict.Inactive>(PS_ETL)

        val answer = toolset.findEndpoint(urlPattern = PS_ETL_PATH, projectPath = projectPath())

        assertEquals("INACTIVE", beanConditionOf(mapper.readTree(answer)["endpoints"].single())["state"]?.asText())
        assertTrue("answer of ${answer.length} chars exceeds $CLIENT_ROOT_BUDGET: $answer", answer.length <= CLIENT_ROOT_BUDGET)
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
        mapper.readTree(
            toolset.getHttpEndpoints(projectPath = projectPath(), endpointType = "ACTUATOR", compact = compact)
        )["endpoints"].toList()

    private companion object {
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val GATING_KEY = "clickhouse.enabled"
        const val EXPOSE_ALL = "management.endpoints.web.exposure.include=*"
        const val PS_ETL = "com.app.PsEtlEndpoint"
        const val PS_ETL_PATH = "/actuator/psEtl"
        const val CH_ETL = "com.app.ChEtlEndpoint"
        const val CH_ETL_PATH = "/actuator/chEtl"
        const val CLICKHOUSE_CONFIG = "com.app.ClickhouseEndpointConfig"
        const val HEALTH_ENDPOINT = "org.springframework.boot.health.actuate.endpoint.HealthEndpoint"
        const val CLIENT_ROOT_BUDGET = 2000
    }
}
