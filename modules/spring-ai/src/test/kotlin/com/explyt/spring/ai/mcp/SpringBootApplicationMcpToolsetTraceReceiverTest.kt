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
import com.intellij.pom.java.LanguageLevel
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.testFramework.PlatformTestUtil
import com.intellij.testFramework.builders.JavaModuleFixtureBuilder
import com.intellij.testFramework.fixtures.JavaCodeInsightFixtureTestCase
import kotlinx.coroutines.runBlocking
import java.io.File

class SpringBootApplicationMcpToolsetTraceReceiverTest : JavaCodeInsightFixtureTestCase() {

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    override fun tuneFixture(moduleBuilder: JavaModuleFixtureBuilder<*>) {
        moduleBuilder.addJdkVersion(LanguageLevel.JDK_21)
    }

    override fun setUp() {
        super.setUp()
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            listOf(TestLibrary.kotlin_1_9_22, TestLibrary.springContext_6_0_7).forEach {
                addFromMaven(model, it.mavenCoordinates, it.includeTransitiveDependencies)
            }
        }
        val root = File(project.basePath!!, ROOT).apply { mkdirs() }
        mapOf("Billing.kt" to BILLING_SOURCE, "Extensions.kt" to EXTENSIONS_SOURCE, "JavaBilling.java" to JAVA_SOURCE).forEach { (name, content) ->
            File(root, "com/example/billing/$name").apply {
                parentFile.mkdirs()
                writeText(content)
            }
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        val rootVf = VfsUtil.findFile(root.toPath(), true) ?: error("Source root not visible: $root")
        ModuleRootModificationUtil.updateModel(myFixture.module) { model ->
            model.addContentEntry(rootVf).addSourceFolder(rootVf, false)
        }
        WriteAction.runAndWait<Throwable> { LocalFileSystem.getInstance().refresh(false) }
        PlatformTestUtil.dispatchAllInvocationEventsInIdeEventQueue()
        IndexingTestUtil.waitUntilIndexesAreReady(project)
    }

    fun testTopLevelExtensionInAnotherFileReportsItsDeclaredReceiver() = runBlocking {
        val node = nodeReachedFrom("fun chargeAccount", "charge")
        assertEquals("$ROOT/com/example/billing/Extensions.kt", node["filePath"].asText())
        assertEquals(listOf("amount"), parametersOf(node))
        assertReceiver("BillingAccount", node)
    }

    fun testObjectMemberExtensionReportsItsExtensionReceiverNotTheDispatchReceiver() = runBlocking {
        val node = nodeReachedFrom("fun chargeWithHelper", "chargeWithPolicy")
        assertEquals(listOf("amount"), parametersOf(node))
        assertReceiver("BillingAccount", node)
    }

    fun testGenericExtensionKeepsTheDeclaredTypeParameterRatherThanItsBound() = runBlocking {
        val node = nodeReachedFrom("fun touchAccount", "touch")
        assertEquals(emptyList<String>(), parametersOf(node))
        assertReceiver("T", node)
    }

    fun testNullableExtensionKeepsTheQuestionMark() = runBlocking {
        val node = nodeReachedFrom("fun normalizeLabel", "orBlank")
        assertEquals(emptyList<String>(), parametersOf(node))
        assertReceiver("String?", node)
    }

    fun testOrdinaryMemberMethodHasNoReceiverKey() = runBlocking {
        val node = nodeReachedFrom("fun readBalance", "balance")
        assertEquals(listOf("currency"), parametersOf(node))
        assertFalse("Ordinary methods must omit receiver, not serialize null: $node", node.has("receiver"))
    }

    fun testSuspendExtensionReportsReceiverWithoutJvmContinuationOrReceiverParameters() = runBlocking {
        val node = nodeReachedFrom("suspend fun chargeLater", "chargeSuspending")
        assertEquals(listOf("amount"), parametersOf(node))
        assertFalse("The JVM continuation must not leak into parameters", parametersOf(node).any { it.contains('$') })
        assertReceiver("BillingAccount", node)
    }

    fun testOrdinaryNodeRetainsTheBaselineOrderedKeys() = runBlocking {
        assertEquals(BASELINE_KEYS, nodeReachedFrom("fun readBalance", "balance").fieldNames().asSequence().toList())
    }

    fun testExtensionNodeInsertsReceiverImmediatelyAfterParameters() = runBlocking {
        val keys = BASELINE_KEYS.toMutableList().apply { add(indexOf("parameters") + 1, "receiver") }
        assertEquals(keys, nodeReachedFrom("fun chargeAccount", "charge").fieldNames().asSequence().toList())
    }

    fun testJavaMethodOmitsReceiver() = runBlocking {
        val node = nodeReachedFrom("fun javaBalance", "available")
        assertEquals(listOf("currency"), parametersOf(node))
        assertFalse("Java methods must omit receiver: $node", node.has("receiver"))
    }

    fun testQualifiedReceiverKeepsItsWrittenPackage() = runBlocking {
        assertReceiver("com.example.billing.BillingAccount", nodeReachedFrom("fun qualifiedCharge", "chargeQualified"))
    }

    fun testTypealiasReceiverKeepsTheAlias() = runBlocking {
        assertReceiver("Money", nodeReachedFrom("fun formatMoney", "format"))
    }

    fun testFunctionTypeReceiverKeepsParentheses() = runBlocking {
        val node = nodeReachedFrom("fun runCallback", "runWith")
        assertEquals(listOf("x"), parametersOf(node))
        assertReceiver("((String) -> Unit)", node)
    }

    fun testClassMemberExtensionReportsReceiverWhenCalledInsideItsClass() = runBlocking {
        assertReceiver("BillingAccount", nodeReachedFrom("fun memberCharge", "chargeInClass"))
    }

    private suspend fun nodeReachedFrom(anchor: String, name: String): JsonNode {
        val offset = BILLING_SOURCE.indexOf(anchor)
        assertTrue("Precondition: service declaration exists for $anchor", offset >= 0)
        val result = mapper.readTree(
            toolset.traceCallChain(
                filePath = "$ROOT/com/example/billing/Billing.kt",
                line = BILLING_SOURCE.take(offset).count { it == '\n' } + 1,
                projectPath = project.basePath!!,
                depth = 2,
                includeTests = false,
            )
        )
        val chain = result["chain"] ?: error("Precondition: trace has a chain: $result")
        val nodes = chain.filter { it["methodName"].asText() == name }
        assertEquals("Precondition: exactly one $name node is traced: $result", 1, nodes.size)
        val node = nodes.single()
        assertTrue(
            "Precondition: service call links to the traced $name node: $result",
            chain[0]["callsInto"].any { !it["node"].isNull && it["node"].asInt() == node["id"].asInt() }
        )
        return node
    }

    private fun parametersOf(node: JsonNode): List<String> = node["parameters"].map { it.asText() }

    private fun assertReceiver(expected: String, node: JsonNode) {
        assertEquals("The trace node must report the declared extension receiver", expected, node["receiver"]?.asText())
    }

    private companion object {
        const val ROOT = "traceReceivers"
        val BASELINE_KEYS = listOf("id", "layer", "reachedBy", "className", "methodName", "filePath", "line", "parameters", "aop", "callsInto")

        val JAVA_SOURCE = """
            package com.example.billing;
            public class JavaBilling {
                public long available(String currency) { return currency.length(); }
            }
        """.trimIndent()

        val BILLING_SOURCE = """
            package com.example.billing

            import org.springframework.stereotype.Service

            interface Entity
            class Receipt(val amount: Long)
            class BillingAccount : Entity {
                fun balance(currency: String): Long = currency.length.toLong()
            }

            object BillingHelper {
                fun BillingAccount.chargeWithPolicy(amount: Long): Receipt = Receipt(amount)
            }

            @Service
            class BillingService {
                fun chargeAccount(account: BillingAccount): Receipt = account.charge(1)
                fun chargeWithHelper(account: BillingAccount): Receipt = with(BillingHelper) {
                    account.chargeWithPolicy(1)
                }
                fun touchAccount(account: BillingAccount): Entity = account.touch()
                fun normalizeLabel(label: String?): String = label.orBlank()
                fun readBalance(account: BillingAccount): Long = account.balance("credits")
                suspend fun chargeLater(account: BillingAccount): Receipt = account.chargeSuspending(1)
                fun javaBalance(billing: JavaBilling): Long = billing.available("credits")
                fun qualifiedCharge(account: BillingAccount): Receipt = account.chargeQualified(1)
                fun formatMoney(amount: Money): String = amount.format()
                fun runCallback(callback: (String) -> Unit): Unit = callback.runWith("charge")
                fun memberCharge(account: BillingAccount): Receipt = account.chargeInClass(1)
                private fun BillingAccount.chargeInClass(amount: Long): Receipt = Receipt(amount)
            }
        """.trimIndent()

        val EXTENSIONS_SOURCE = """
            package com.example.billing

            fun BillingAccount.charge(amount: Long): Receipt = Receipt(amount)
            fun <T : Entity> T.touch(): T = this
            fun String?.orBlank(): String = this ?: ""
            suspend fun BillingAccount.chargeSuspending(amount: Long): Receipt = Receipt(amount)
            fun com.example.billing.BillingAccount.chargeQualified(amount: Long): Receipt = Receipt(amount)
            typealias Money = Long
            fun Money.format(): String = toString()
            fun ((String) -> Unit).runWith(x: String): Unit = invoke(x)
        """.trimIndent()
    }
}
