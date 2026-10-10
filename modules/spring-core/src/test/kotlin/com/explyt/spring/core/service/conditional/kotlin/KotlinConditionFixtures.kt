/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.intellij.testFramework.fixtures.CodeInsightTestFixture

class KotlinConditionFixtures(private val fixture: CodeInsightTestFixture) : ConditionFixtures {

    override fun addApplication() = addKotlin(
        "Application",
        """
        package com.app

        import org.springframework.boot.autoconfigure.SpringBootApplication

        @SpringBootApplication
        class Application
        """
    )

    override fun addPlainComponent() = addKotlin(
        "PlainService",
        """
        package com.app

        import org.springframework.stereotype.Component

        @Component
        class PlainService
        """
    )

    override fun addPropertyGatedConfiguration(className: String) = addKotlin(
        className,
        """
        package com.app

        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
        import org.springframework.context.annotation.Configuration

        @Configuration
        @ConditionalOnProperty(name = ["feature.sync"], havingValue = "true")
        class $className
        """
    )

    override fun addCustomConditionConfiguration() {
        addKotlin(
            "MyCondition",
            """
            package com.app

            import org.springframework.context.annotation.Condition
            import org.springframework.context.annotation.ConditionContext
            import org.springframework.core.type.AnnotatedTypeMetadata

            class MyCondition : Condition {
                override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = false
            }
            """
        )
        addKotlin(
            "CustomConditionConfig",
            """
            package com.app

            import org.springframework.context.annotation.Conditional
            import org.springframework.context.annotation.Configuration

            @Configuration
            @Conditional(MyCondition::class)
            class CustomConditionConfig
            """
        )
    }

    override fun addUnknownProfileConfiguration() = addKotlin(
        "UnknownProfileConfig",
        """
        package com.app

        import org.springframework.context.annotation.Configuration
        import org.springframework.context.annotation.Profile

        @Configuration
        @Profile(UNKNOWN_PROFILE)
        class UnknownProfileConfig
        """
    )

    override fun addMissingBeanConfiguration() {
        addKotlin(
            "Missing",
            """
            package com.app

            class Missing
            """
        )
        addKotlin(
            "MissingBeanConfig",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnBean(Missing::class)
            class MissingBeanConfig
            """
        )
    }

    override fun addConditionalOuterConfiguration() {
        addKotlin(
            "OuterService",
            """
            package com.app

            class OuterService
            """
        )
        addKotlin(
            "OuterConfig",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            @ConditionalOnProperty(name = ["outer.enabled"], havingValue = "true")
            class OuterConfig {
                @Bean
                fun outerService(): OuterService = OuterService()
            }
            """
        )
    }

    override fun addMixedConditionsConfiguration() {
        addKotlin(
            "MixedCondition",
            """
            package com.app

            import org.springframework.context.annotation.Condition
            import org.springframework.context.annotation.ConditionContext
            import org.springframework.core.type.AnnotatedTypeMetadata

            class MixedCondition : Condition {
                override fun matches(context: ConditionContext, metadata: AnnotatedTypeMetadata): Boolean = true
            }
            """
        )
        addKotlin(
            "MixedConditionsConfig",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Conditional
            import org.springframework.context.annotation.Configuration

            @Configuration
            @Conditional(MixedCondition::class)
            @ConditionalOnProperty(name = ["mixed.enabled"], havingValue = "true")
            class MixedConditionsConfig
            """
        )
    }

    override fun addSyncAdminController() = addKotlin(
        "SyncAdminController",
        """
        package com.app

        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
        import org.springframework.stereotype.Controller

        @Controller
        @ConditionalOnProperty(prefix = "admin.sync", name = ["enabled"])
        class SyncAdminController
        """
    )

    private fun addKotlin(className: String, source: String) {
        fixture.addFileToProject("com/app/$className.kt", source.trimIndent())
    }
}
