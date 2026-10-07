/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.ConditionalOnPropertyBootSemanticsTestCase

abstract class JavaPropertyConditionTestCase : ConditionalOnPropertyBootSemanticsTestCase() {
    protected open val conditionAnnotations: List<String> = listOf("ConditionalOnProperty")

    override fun setUp() {
        super.setUp()
        addJava(
            "Application",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class Application {}
            """
        )
    }

    protected fun addJava(className: String, source: String) {
        myFixture.addFileToProject("com/app/$className.java", source.trimIndent())
    }

    protected fun addConfiguration(className: String, conditions: String) {
        val imports = conditionAnnotations.joinToString("\n") {
            "import org.springframework.boot.autoconfigure.condition.$it;"
        }
        myFixture.addFileToProject(
            "com/app/$className.java",
            "package com.app;\n\n$imports\nimport org.springframework.context.annotation.Configuration;\n\n" +
                    "@Configuration\n${conditions.trimIndent()}\npublic class $className {}\n"
        )
    }
}
