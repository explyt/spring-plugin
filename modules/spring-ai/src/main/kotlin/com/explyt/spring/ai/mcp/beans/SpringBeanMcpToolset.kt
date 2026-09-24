/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp.beans

import com.explyt.spring.core.service.beans.BeanQueryException
import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool
import com.intellij.mcpserver.annotations.McpToolHintValue.TRUE
import com.intellij.mcpserver.annotations.McpToolHints

class SpringBeanMcpToolset : McpToolset {

    @McpTool("explyt_find_spring_bean", title = "Find Spring beans or resolve an injection point")
    @McpToolHints(readOnlyHint = TRUE, idempotentHint = TRUE)
    @McpDescription(
        description = "Call before adding a bean by type or name, to see which beans already answer to it and " +
                "whether one already exists; and after writing a constructor or field, to check what Spring " +
                "would inject into that one parameter. " +
                "Answers the question a text search cannot: which beans the IDE's model holds for one named " +
                "application, and which of them a given injection point selects once @Qualifier, @Primary and " +
                "the declared shape are applied - a grep over '@Bean' shows the annotations and says nothing " +
                "about what is selected. " +
                "Two modes: supply typeFqn and/or beanName to look beans up, or filePath and line (and column " +
                "when several declarations share the line) to inspect an injection point; mixing them is " +
                "rejected rather than guessed. " +
                "The answer is relative to one explicitly chosen model, named in 'model': STATIC is the IDE's " +
                "estimate for the application's module and does not prove component-scan or profile isolation, " +
                "NATIVE_SNAPSHOT is a recorded context that may be out of date - neither proves the " +
                "application starts or that injection succeeds at runtime. " +
                "'outcome' is INDETERMINATE, and 'matchCompleteness' PARTIAL, whenever something could not be " +
                "decided; 'NONE' means nothing matched in that model, never that no declaration exists. " +
                "For an injection point, 'required' and 'hasDefaultValue' are independent of whether a " +
                "candidate exists, and null means unknown rather than optional. " +
                "Returns at most 'limit' candidates (5 by default) within 'maxChars' of compact JSON (1800 by " +
                "default); when 'truncated' is true, repeat the call with 'offset' = 'nextOffset' and " +
                "'expectedRevision' = 'revision' to continue the same answer. " +
                "Take applicationClassName from explyt_get_spring_boot_applications; " +
                "explyt_get_project_beans_by_spring_boot_application lists a whole stereotype instead of " +
                "answering about one bean or one injection point."
    )
    suspend fun findSpringBean(
        @McpDescription(
            "Path to the root of the open project; another open project is never answered instead. " +
                    "Omit it when a single project is open; when several are, it is required"
        )
        projectPath: String? = null,
        @McpDescription("FQN of the @SpringBootApplication; required when the scope holds more than one")
        applicationClassName: String? = null,
        @McpDescription("Which model answers: AUTO (default), STATIC, or NATIVE")
        source: String = "AUTO",
        @McpDescription("Opaque id of a loaded native context, taken from a CONTEXT_REQUIRED answer")
        contextId: String? = null,
        @McpDescription("LOOKUP: exact FQN of a class or interface; compatible beans are listed, not substrings")
        typeFqn: String? = null,
        @McpDescription("LOOKUP: exact case-sensitive bean name; an alias resolves to its canonical bean")
        beanName: String? = null,
        @McpDescription("INJECTION: project-relative path of the file holding the injection point")
        filePath: String? = null,
        @McpDescription("INJECTION: 1-based line of the field or parameter")
        line: Int? = null,
        @McpDescription("INJECTION: 1-based column, needed only when several declarations share the line")
        column: Int? = null,
        @McpDescription("Index of the first candidate to return; needs expectedRevision when above 0")
        offset: Int = 0,
        @McpDescription("Maximum candidates on this page, 1..50, 5 by default")
        limit: Int = 5,
        @McpDescription("Budget of the whole compact JSON answer, 512..16000, 1800 by default")
        maxChars: Int = 1800,
        @McpDescription("The 'revision' of the first page, required to continue that same answer")
        expectedRevision: String? = null,
        @McpDescription("Adds known aliases, qualifiers, primary, profiles and conditions to each candidate")
        includeDetails: Boolean = false
    ): String {
        val request = BeanLookupRequest(
            applicationClassName = applicationClassName,
            source = source,
            contextId = contextId,
            typeFqn = typeFqn,
            beanName = beanName,
            filePath = filePath,
            line = line,
            column = column,
            includeDetails = includeDetails
        )
        val page = BeanPageRequest(
            offset = offset,
            limit = limit,
            maxChars = maxChars,
            expectedRevision = expectedRevision
        )

        val project = try {
            BeanLookupService.resolveProject(projectPath)
        } catch (e: BeanQueryException) {
            return BoundedBeanResponseWriter()
                .writeError(e.problem, BoundedBeanResponseWriter.FALLBACK_BUDGET)
        }
        return BeanLookupService.getInstance(project).query(request, page)
    }
}
