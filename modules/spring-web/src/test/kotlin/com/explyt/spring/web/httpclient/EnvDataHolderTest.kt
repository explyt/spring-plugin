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

package com.explyt.spring.web.httpclient

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.openapi.vfs.LocalFileSystem
import java.nio.file.Files
import kotlin.io.path.writeText

class EnvDataHolderTest : ExplytJavaLightTestCase() {

    fun testSelectFileLoadsJsonEnvironments() {
        val tempDir = Files.createTempDirectory("env-data-holder-test")
        val httpFile = tempDir.resolve("requests.http").apply {
            writeText("GET http://localhost")
        }
        val envFile = tempDir.resolve("http-client.env.json").apply {
            writeText(
                """
                {
                  "dev": {
                    "host": "localhost"
                  },
                  "prod": {
                    "host": "example.com"
                  }
                }
                """.trimIndent()
            )
        }
        val envVirtualFile = LocalFileSystem.getInstance().refreshAndFindFileByNioFile(envFile)
        assertNotNull("Env file must be visible to VFS", envVirtualFile)

        val holder = EnvDataHolder(httpFile)
        holder.addFile(envVirtualFile!!, project)

        assertTrue(holder.envFileIsJson.get())
        assertEquals(listOf("", "dev", "prod"), holder.envModel.items)
    }
}
