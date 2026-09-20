/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.ai.mcp

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.fasterxml.jackson.databind.ObjectMapper
import kotlinx.coroutines.runBlocking

class SpringBootApplicationMcpToolsetWebFluxTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springReactiveWeb_3_1_1)

    private val toolset = SpringBootApplicationMcpToolset()
    private val mapper = ObjectMapper()

    private fun projectPath(): String = project.basePath ?: ""

    fun testRequestParamMultipartFileRemainsQueryOnWebFlux() = runBlocking<Unit> {
        myFixture.addFileToProject("com/example/app/web/ReactiveController.java", """
            package com.example.app.web;

            import org.springframework.web.bind.annotation.PostMapping;
            import org.springframework.web.bind.annotation.RequestPart;
            import org.springframework.web.bind.annotation.RequestParam;
            import org.springframework.web.bind.annotation.RestController;
            import org.springframework.web.multipart.MultipartFile;

            @RestController
            public class ReactiveController {
                @PostMapping("/api/reactive/upload")
                public String upload(
                        @RequestParam("file") MultipartFile file,
                        @RequestPart("metadata") String metadata) {
                    return file.getName() + metadata;
                }
            }
        """.trimIndent())

        val result = mapper.readTree(toolset.getEndpointContract(
            urlPattern = "/api/reactive/upload",
            projectPath = projectPath(),
        ))
        val parameters = result["endpoints"].single()["parameters"]
        val byName = parameters.associateBy { it["name"].asText() }

        assertEquals("QUERY", byName.getValue("file")["source"].asText())
        assertEquals("PART", byName.getValue("metadata")["source"].asText())
    }
}
