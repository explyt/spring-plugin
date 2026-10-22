/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.service.RuntimeClasspathLibraryBeansTestCase
import org.intellij.lang.annotations.Language

class RuntimeClasspathLibraryBeansTest : RuntimeClasspathLibraryBeansTestCase() {

    override val applicationFileName = "com/example/app/GatewayApplication.kt"

    @Language("kotlin")
    override val applicationText = """
        package com.example.app

        import org.springframework.boot.autoconfigure.SpringBootApplication

        @SpringBootApplication
        class GatewayApplication
    """.trimIndent()

    override val consumerFileName = "com/example/app/AdvisorConsumer.kt"

    @Language("kotlin")
    override val consumerText = """
        package com.example.app

        import org.springframework.aop.Advisor
        import org.springframework.beans.factory.annotation.Autowired
        import org.springframework.beans.factory.annotation.Qualifier
        import org.springframework.stereotype.Component

        @Component
        class AdvisorConsumer {
            @Autowired
            @Qualifier("org.springframework.transaction.config.internalTransactionAdvisor")
            lateinit var advisor: Advisor
        }
    """.trimIndent()

    override val testConsumerFileName = "com/example/app/AdvisorTestConsumer.kt"

    @Language("kotlin")
    override val testConsumerText = """
        package com.example.app

        import org.springframework.aop.Advisor
        import org.springframework.beans.factory.annotation.Autowired
        import org.springframework.beans.factory.annotation.Qualifier
        import org.springframework.stereotype.Component

        @Component
        class AdvisorTestConsumer {
            @Autowired
            @Qualifier("org.springframework.transaction.config.internalTransactionAdvisor")
            lateinit var advisor: Advisor
        }
    """.trimIndent()
}
