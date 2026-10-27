/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.kotlin

import com.explyt.spring.web.providers.UnreadableMappingPathUrlTestCase

class UnreadableMappingPathUrlTest : UnreadableMappingPathUrlTestCase() {

    override val unreadableMethods = listOf("unresolved")

    override val readableUrls = linkedMapOf(
        "pathless" to "/api",
        "empty" to "/api",
        "literal" to "/api/x",
        "constant" to "/api/const",
    )

    override fun addController() {
        myFixture.addFileToProject(
            "com/example/Paths.kt", """
            package com.example

            object Paths {
                const val CONST = "/const"
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ApiController.kt", """
            package com.example

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/api")
            class ApiController {
                @GetMapping(Missing.PATH)
                fun unresolved(): String = ""

                @GetMapping
                fun pathless(): String = ""

                @GetMapping("")
                fun empty(): String = ""

                @GetMapping("/x")
                fun literal(): String = ""

                @GetMapping(Paths.CONST)
                fun constant(): String = ""
            }
            """.trimIndent()
        )
    }
}
