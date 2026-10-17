/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.providers.java

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
            "com/example/Paths.java", """
            package com.example;

            public final class Paths {
                public static final String CONST = "/const";
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/example/ApiController.java", """
            package com.example;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/api")
            public class ApiController {
                @GetMapping(Missing.PATH)
                public String unresolved() { return ""; }

                @GetMapping
                public String pathless() { return ""; }

                @GetMapping("")
                public String empty() { return ""; }

                @GetMapping("/x")
                public String literal() { return ""; }

                @GetMapping(Paths.CONST)
                public String constant() { return ""; }
            }
            """.trimIndent()
        )
    }
}
