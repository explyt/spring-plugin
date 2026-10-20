/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.CompiledPropertyConditionTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures

class CompiledPropertyConditionTest : CompiledPropertyConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }

    override fun addProjectConfiguration(condition: PropertyCondition) {
        myFixture.addFileToProject(
            "com/app/$PROJECT_CONFIGURATION.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            ${condition.kotlin}
            open class $PROJECT_CONFIGURATION {
                @Bean
                open fun projectService(): StringBuilder = StringBuilder()
            }
            """.trimIndent()
        )
    }
}
