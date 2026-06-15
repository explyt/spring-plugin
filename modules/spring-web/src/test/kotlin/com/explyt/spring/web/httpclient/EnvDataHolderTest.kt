/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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
