/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.CompiledBeanConditionTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures

class CompiledBeanConditionTest : CompiledBeanConditionTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }

    override fun addProjectFactoryOf(libraryClass: String) {
        myFixture.addFileToProject(
            "com/app/FooConfiguration.kt",
            """
            package com.app

            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            open class FooConfiguration {
                @Bean
                open fun foo(): $libraryClass = $libraryClass()
            }
            """.trimIndent()
        )
    }
}
