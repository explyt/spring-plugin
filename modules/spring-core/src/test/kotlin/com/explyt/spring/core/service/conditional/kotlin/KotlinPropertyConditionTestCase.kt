/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.ConditionalOnPropertyBootSemanticsTestCase

abstract class KotlinPropertyConditionTestCase : ConditionalOnPropertyBootSemanticsTestCase() {
    protected open val conditionAnnotations: List<String> = listOf("ConditionalOnProperty")

    override fun setUp() {
        super.setUp()
        addKotlin(
            "Application",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class Application
            """
        )
    }

    protected fun addKotlin(className: String, source: String) {
        myFixture.addFileToProject("com/app/$className.kt", source.trimIndent())
    }

    protected fun addConfiguration(className: String, conditions: String) {
        val imports = conditionAnnotations.joinToString("\n") {
            "import org.springframework.boot.autoconfigure.condition.$it"
        }
        myFixture.addFileToProject(
            "com/app/$className.kt",
            "package com.app\n\n$imports\nimport org.springframework.context.annotation.Configuration\n\n" +
                    "@Configuration\n${conditions.trimIndent()}\nclass $className\n"
        )
    }
}
