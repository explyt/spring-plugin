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

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.mcpserver.McpToolCallResult
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializerOrNull
import kotlin.reflect.typeOf

class McpToolTest : ExplytJavaLightTestCase() {

    fun testPackageScan() {
        /*val user = UserSession("John", 30)
        val jsonString = Json.encodeToString(user)*/
        val specificType = typeOf<UserSession>()
        val serializerOrNull = serializerOrNull(specificType)

        SpringBootApplications("vv")
    }

}

