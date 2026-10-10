/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.web.loader.ActuatorEndpointBeanConditionTestCase

class ActuatorEndpointBeanConditionTest : ActuatorEndpointBeanConditionTestCase() {

    override fun getTestDataPath() = "testdata/java/"

    override fun addApplication() {
        myFixture.addFileToProject(
            "com/app/DemoApplication.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class DemoApplication {}
            """.trimIndent()
        )
    }

    override fun addPropertyGatedComponentEndpoint() {
        myFixture.addFileToProject(
            "com/app/PsEtlEndpoint.java",
            """
            package com.app;

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.stereotype.Component;

            @Component
            @Endpoint(id = "psEtl")
            @ConditionalOnProperty(name = "clickhouse.enabled", havingValue = "true")
            public class PsEtlEndpoint {
                @ReadOperation
                public String status() { return "idle"; }
            }
            """.trimIndent()
        )
    }

    override fun addUnconditionalComponentEndpoint() {
        myFixture.addFileToProject(
            "com/app/CacheStatsEndpoint.java",
            """
            package com.app;

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.stereotype.Component;

            @Component
            @Endpoint(id = "cachestats")
            public class CacheStatsEndpoint {
                @ReadOperation
                public String stats() { return "ok"; }
            }
            """.trimIndent()
        )
    }

    override fun addEndpointRegisteredByGatedConfiguration() {
        myFixture.addFileToProject(
            "com/app/ChEtlEndpoint.java",
            """
            package com.app;

            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;

            @Endpoint(id = "chEtl")
            public class ChEtlEndpoint {
                @ReadOperation
                public String status() { return "idle"; }
            }
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/app/ClickhouseEndpointConfig.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            @ConditionalOnProperty(name = "clickhouse.enabled", havingValue = "true")
            public class ClickhouseEndpointConfig {
                @Bean
                public ChEtlEndpoint chEtlEndpoint() { return new ChEtlEndpoint(); }
            }
            """.trimIndent()
        )
    }

    override fun addRestController() {
        myFixture.addFileToProject(
            "com/app/EtlController.java",
            """
            package com.app;

            import org.springframework.web.bind.annotation.GetMapping;
            import org.springframework.web.bind.annotation.RequestMapping;
            import org.springframework.web.bind.annotation.RestController;

            @RestController
            @RequestMapping("/etl")
            public class EtlController {
                @GetMapping("/runs")
                public String runs() { return "runs"; }
            }
            """.trimIndent()
        )
    }
}
