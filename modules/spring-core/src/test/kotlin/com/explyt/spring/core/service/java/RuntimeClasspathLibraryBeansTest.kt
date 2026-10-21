/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.service.RuntimeClasspathLibraryBeansTestCase
import org.intellij.lang.annotations.Language

class RuntimeClasspathLibraryBeansTest : RuntimeClasspathLibraryBeansTestCase() {

    override val applicationFileName = "com/example/app/GatewayApplication.java"

    @Language("java")
    override val applicationText = """
        package com.example.app;

        import org.springframework.boot.autoconfigure.SpringBootApplication;

        @SpringBootApplication
        public class GatewayApplication {
        }
    """.trimIndent()

    override val consumerFileName = "com/example/app/AdvisorConsumer.java"

    @Language("java")
    override val consumerText = """
        package com.example.app;

        import org.springframework.aop.Advisor;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.beans.factory.annotation.Qualifier;
        import org.springframework.stereotype.Component;

        @Component
        public class AdvisorConsumer {
            @Autowired
            @Qualifier("org.springframework.transaction.config.internalTransactionAdvisor")
            Advisor advisor;
        }
    """.trimIndent()

    override val testConsumerFileName = "com/example/app/AdvisorTestConsumer.java"

    @Language("java")
    override val testConsumerText = """
        package com.example.app;

        import org.springframework.aop.Advisor;
        import org.springframework.beans.factory.annotation.Autowired;
        import org.springframework.beans.factory.annotation.Qualifier;
        import org.springframework.stereotype.Component;

        @Component
        public class AdvisorTestConsumer {
            @Autowired
            @Qualifier("org.springframework.transaction.config.internalTransactionAdvisor")
            Advisor advisor;
        }
    """.trimIndent()
}
