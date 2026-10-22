/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.ai.mcp.beans.SpringBeanMcpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.reflect.KFunction
import kotlin.reflect.full.findAnnotation
import kotlin.reflect.full.functions
import kotlin.reflect.full.hasAnnotation

/**
 * The tool descriptors are the only thing an agent sees before deciding whether to call a tool, so their shape
 * is a contract of the toolset, not decoration. These checks cover the parts a review cannot be trusted to
 * catch on every new tool.
 */
class SpringBootApplicationMcpToolsetDescriptorTest {

    /**
     * Every tool the plugin publishes, not one toolset's worth.
     *
     * The checks below are contracts of the tools an agent sees, so a second toolset that skipped them would be
     * exactly as broken while this test stayed green.
     */
    private val tools: List<KFunction<*>> =
        listOf(SpringBootApplicationMcpToolset::class, SpringBeanMcpToolset::class)
            .flatMap { toolset -> toolset.functions.filter { it.hasAnnotation<McpTool>() } }

    private val KFunction<*>.toolName: String get() = findAnnotation<McpTool>()!!.name

    private val KFunction<*>.toolDescription: String get() = findAnnotation<McpDescription>()!!.description

    @Test
    fun `every tool is registered`() {
        assertEquals(
            setOf(
                "explyt_get_spring_boot_applications",
                "explyt_get_spring_beans",
                "explyt_find_spring_endpoint",
                "explyt_get_spring_http_endpoints",
                "explyt_get_spring_endpoint_contract",
                "explyt_trace_spring_call_chain",
                "explyt_get_spring_data_entities",
                "explyt_find_spring_bean",
            ),
            tools.map { it.toolName }.toSet()
        )
    }

    /**
     * A client exposes a tool as its server prefix plus the tool name, and many cap that at 64 characters. A name
     * over the cap is mangled - the prefix cut, the name cut short - so the agent can neither find the tool by the
     * pattern its siblings follow nor guess it, and a guess fails as an unknown tool.
     */
    @Test
    fun `every tool name leaves room for a client prefix`() {
        for (tool in tools) {
            assertTrue(
                "${tool.toolName} is ${tool.toolName.length} characters, over the budget of $MAX_TOOL_NAME_LENGTH",
                tool.toolName.length <= MAX_TOOL_NAME_LENGTH
            )
        }
    }

    @Test
    fun `no bean description claims profile-inactive beans are hidden`() {
        val beanTools = tools.filter { it.toolName in BEAN_TOOLS }
        assertEquals(BEAN_TOOLS, beanTools.map { it.toolName }.toSet())
        for (tool in beanTools) {
            assertTrue(
                "${tool.toolName} still says profile-inactive beans are filtered earlier",
                !tool.toolDescription.contains("filtered earlier")
            )
        }
    }

    /**
     * The descriptions hand the agent from one tool to the next by name ("take applicationClassName from
     * explyt_get_spring_boot_applications"). A renamed tool leaves such a hand-off pointing at nothing, and the
     * agent finds out only when the call fails.
     */
    @Test
    fun `every tool a description refers to exists`() {
        val registered = tools.map { it.toolName }.toSet()
        for (tool in tools) {
            val referenced = TOOL_NAME.findAll(tool.toolDescription).map { it.value }.toSet()
            assertEquals(
                "${tool.toolName} refers to a tool that is not registered",
                emptySet<String>(),
                referenced - registered
            )
        }
    }

    /**
     * A description that only states what the tool returns is read by an agent as a lookup and skipped while it
     * is writing code: the moment to call the tool has to be in the text, and it has to lead, because the first
     * sentences are what a tool router and a skimming agent match against.
     */
    @Test
    fun `endpoint descriptions explain ALL http methods`() {
        for (toolName in ENDPOINT_TOOL_NAMES) {
            val description = toolNamed(toolName).toolDescription
            assertTrue(
                "$toolName must explain the ALL httpMethods value",
                description.contains(ALL_METHODS_FRAGMENT)
            )
            assertTrue(
                "$toolName must say that ALL answers every HTTP method",
                description.contains("every HTTP method", ignoreCase = true)
            )
        }
    }

    @Test
    fun `endpoint method filters explain ALL matches every filter value`() {
        for (toolName in FILTERED_ENDPOINT_TOOL_NAMES) {
            val parameter = toolNamed(toolName).parameters.single { it.name == "httpMethod" }
            val description = parameter.findAnnotation<McpDescription>()!!.description
            assertTrue(
                "$toolName httpMethod filter must explain ALL matching every HTTP method",
                description.contains("every HTTP method", ignoreCase = true) &&
                        description.contains(ALL_METHODS_FRAGMENT)
            )
        }
    }

    /**
     * An endpoint answer is as unbounded as the project is large, and a client cuts an answer it cannot hold
     * silently: the endpoint tools take the budget and continuation arguments the bean, entity and trace tools take.
     */
    @Test
    fun `endpoint tools document their budget and continuation`() {
        for (toolName in ENDPOINT_TOOL_NAMES) {
            val tool = toolNamed(toolName)
            for (argument in PAGE_ARGUMENTS) {
                val parameter = tool.parameters.singleOrNull { it.name == argument }
                assertTrue("$toolName has no '$argument' argument", parameter != null)
                assertTrue(
                    "$toolName '$argument' is not described",
                    parameter!!.findAnnotation<McpDescription>()?.description.orEmpty().isNotBlank()
                )
            }
            for (key in listOf("maxChars", "nextOffset", "expectedRevision", "revision")) {
                assertTrue("$toolName description does not name '$key'", tool.toolDescription.contains("'$key'"))
            }
        }
    }

    @Test
    fun `every description leads with the moment to call the tool`() {
        for (tool in tools) {
            val lead = tool.toolDescription.split(". ").take(LEAD_SENTENCES).joinToString(". ")
            assertTrue(
                "${tool.toolName} leads with a capability, not a moment to call it: '$lead'",
                lead.contains(Regex("""\bCall\b"""))
            )
        }
    }

    private fun toolNamed(name: String): KFunction<*> =
        SpringBootApplicationMcpToolset::class.functions.single {
            it.findAnnotation<McpTool>()?.name == name
        }

    private companion object {
        val ENDPOINT_TOOL_NAMES = setOf(
            "explyt_find_spring_endpoint",
            "explyt_get_spring_endpoint_contract",
            "explyt_get_spring_http_endpoints",
        )
        val FILTERED_ENDPOINT_TOOL_NAMES = setOf(
            "explyt_find_spring_endpoint",
            "explyt_get_spring_endpoint_contract",
        )
        const val ALL_METHODS_FRAGMENT = "[\"ALL\"]"
        val PAGE_ARGUMENTS = listOf("maxChars", "offset", "expectedRevision")

        val TOOL_NAME = Regex("""explyt_[a-z_]+""")
        const val LEAD_SENTENCES = 2
        const val MAX_TOOL_NAME_LENGTH = 40
        val BEAN_TOOLS = setOf("explyt_get_spring_beans", "explyt_find_spring_bean")
    }
}
