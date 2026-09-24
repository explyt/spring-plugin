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
                "explyt_get_project_beans_by_spring_boot_application",
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
    fun `every description leads with the moment to call the tool`() {
        for (tool in tools) {
            val lead = tool.toolDescription.split(". ").take(LEAD_SENTENCES).joinToString(". ")
            assertTrue(
                "${tool.toolName} leads with a capability, not a moment to call it: '$lead'",
                lead.contains(Regex("""\bCall\b"""))
            )
        }
    }

    private companion object {
        val TOOL_NAME = Regex("""explyt_[a-z_]+""")
        const val LEAD_SENTENCES = 2
    }
}
