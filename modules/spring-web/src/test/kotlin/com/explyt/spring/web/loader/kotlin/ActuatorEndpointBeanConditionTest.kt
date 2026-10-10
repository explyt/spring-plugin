/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.kotlin

import com.explyt.spring.web.loader.ActuatorEndpointBeanConditionTestCase

class ActuatorEndpointBeanConditionTest : ActuatorEndpointBeanConditionTestCase() {

    override fun getTestDataPath() = "testdata/kotlin/"

    override fun addApplication() {
        myFixture.addFileToProject(
            "com/app/DemoApplication.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class DemoApplication
            """.trimIndent()
        )
    }

    override fun addPropertyGatedComponentEndpoint() {
        myFixture.addFileToProject(
            "com/app/PsEtlEndpoint.kt",
            """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.stereotype.Component

            @Component
            @Endpoint(id = "psEtl")
            @ConditionalOnProperty(name = ["clickhouse.enabled"], havingValue = "true")
            class PsEtlEndpoint {
                @ReadOperation
                fun status() = "idle"
            }
            """.trimIndent()
        )
    }

    override fun addUnconditionalComponentEndpoint() {
        myFixture.addFileToProject(
            "com/app/CacheStatsEndpoint.kt",
            """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.stereotype.Component

            @Component
            @Endpoint(id = "cachestats")
            class CacheStatsEndpoint {
                @ReadOperation
                fun stats() = "ok"
            }
            """.trimIndent()
        )
    }

    override fun addEndpointRegisteredByGatedConfiguration() {
        myFixture.addFileToProject(
            "com/app/ChEtlEndpoint.kt",
            """
            package com.app

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation

            @Endpoint(id = "chEtl")
            class ChEtlEndpoint {
                @ReadOperation
                fun status() = "idle"
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/ClickhouseEndpointConfig.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["clickhouse.enabled"], havingValue = "true")
            class ClickhouseEndpointConfig {
                @Bean
                fun chEtlEndpoint() = ChEtlEndpoint()
            }
            """.trimIndent()
        )
    }

    override fun addRestController() {
        myFixture.addFileToProject(
            "com/app/EtlController.kt",
            """
            package com.app

            import org.springframework.web.bind.annotation.GetMapping
            import org.springframework.web.bind.annotation.RequestMapping
            import org.springframework.web.bind.annotation.RestController

            @RestController
            @RequestMapping("/etl")
            class EtlController {
                @GetMapping("/runs")
                fun runs() = "runs"
            }
            """.trimIndent()
        )
    }
}
