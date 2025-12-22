/*
 * Copyright © 2025 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.ai.mcp

import com.intellij.mcpserver.McpToolset
import com.intellij.mcpserver.annotations.McpDescription
import com.intellij.mcpserver.annotations.McpTool

class TestToolset : McpToolset {

    @McpTool("my_best_tool1")
    @McpDescription(description = "My first tool")
    fun myBestTool1(
        @McpDescription("Path to the project root") projectPath: String
    ): String {
        return "I am best tool from $projectPath"
    }

    @McpTool("my_best_tool2")
    @McpDescription(description = "My second tool")
    fun myBestTool2(
        @McpDescription("Arg1") arg1: String,
        @McpDescription("Arg2") arg2: Int,
    ): String {
        return "I am tool with params $arg1 & $arg2"
    }
}