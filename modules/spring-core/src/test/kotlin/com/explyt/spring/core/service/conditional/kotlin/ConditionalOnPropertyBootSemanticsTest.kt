/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

class ConditionalOnPropertyBootSemanticsTest : KotlinPropertyConditionTestCase() {

    fun testEveryNameMustMatch() {
        addProperties("application.properties", "app.first.enabled=true", "app.second.enabled=false")
        addConfiguration(
            "AllNamesConfig",
            """@ConditionalOnProperty(name = ["app.first.enabled", "app.second.enabled"], havingValue = "true")"""
        )

        assertInactive("com.app.AllNamesConfig", "app.first.enabled", "app.second.enabled")
    }

    fun testHavingValueIsComparedIgnoringCase() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: true")
        addConfiguration("UpperCaseConfig", """@ConditionalOnProperty(name = ["app.feature.enabled"], havingValue = "TRUE")""")

        assertActive("com.app.UpperCaseConfig", "app.feature.enabled")
    }

    fun testFalseValueWithoutHavingValueDoesNotMatch() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: false")
        addConfiguration("FalseFlagConfig", """@ConditionalOnProperty("app.feature.enabled")""")

        assertInactive("com.app.FalseFlagConfig", "app.feature.enabled")
    }

    fun testMissingKeyWithHavingValueAndMatchIfMissingMatches() {
        addProperties("application.properties", "app.marker=present")
        addConfiguration(
            "MissingWithValueConfig",
            """@ConditionalOnProperty(name = ["app.feature.enabled"], havingValue = "true", matchIfMissing = true)"""
        )

        assertActive("com.app.MissingWithValueConfig", "app.marker")
    }

    fun testActiveProfileFileOverridesDefaultFile() {
        addProperties("application.yaml", "spring:", "  profiles:", "    active: prod", "app:", "  feature:", "    enabled: false")
        addProperties("application-prod.yaml", "app:", "  feature:", "    enabled: true")
        addConfiguration("ProdProfileConfig", """@ConditionalOnProperty(name = ["app.feature.enabled"], havingValue = "true")""")

        assertActive("com.app.ProdProfileConfig", "app.feature.enabled")
    }

    fun testActiveProfileDocumentOverridesDefaultDocument() {
        addProperties(
            "application.yaml",
            "spring:", "  profiles:", "    active: prod",
            "x:", "  enabled: false",
            "---",
            "spring:", "  config:", "    activate:", "      on-profile: prod",
            "x:", "  enabled: true"
        )
        addConfiguration("ProdDocumentConfig", """@ConditionalOnProperty(name = ["x.enabled"], havingValue = "true")""")

        assertActive("com.app.ProdDocumentConfig", "x.enabled")
    }

    fun testPlaceholderFalseDefaultMatchesHavingValueFalse() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: \${EXPLYT_UNSET_FEATURE_FLAG:false}")
        addConfiguration(
            "PlaceholderFalseConfig",
            """@ConditionalOnProperty(name = ["app.feature.enabled"], havingValue = "false")"""
        )

        assertActive("com.app.PlaceholderFalseConfig", "app.feature.enabled")
    }

    fun testUnresolvablePlaceholderKeepsTheBean() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: \${EXPLYT_UNSET_FEATURE_FLAG}")
        addConfiguration(
            "UnresolvableConfig",
            """@ConditionalOnProperty(name = ["app.feature.enabled"], havingValue = "true")"""
        )

        assertActive("com.app.UnresolvableConfig", "app.feature.enabled")
    }

    fun testMetaAnnotationMatchIfMissingIsRead() {
        addProperties("application.properties", "app.marker=present")
        addKotlin(
            "MetaMissingConfig",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            @Target(AnnotationTarget.CLASS)
            @Retention(AnnotationRetention.RUNTIME)
            @ConditionalOnProperty(name = ["x"], matchIfMissing = true)
            annotation class EnabledWhen

            @Configuration
            @EnabledWhen
            class MetaMissingConfig
            """
        )

        assertActive("com.app.MetaMissingConfig", "app.marker")
    }

    fun testMethodConditionDoesNotExcludeConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        addKotlin(
            "MethodConfig",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration

            @Configuration
            class MethodConfig {
                @Bean
                @ConditionalOnProperty(name = ["x.enabled"], havingValue = "true")
                fun disabledBean(): String = "disabled"
            }
            """
        )

        assertActive("com.app.MethodConfig", "x.enabled")
        assertBeanMethodExcluded("disabledBean")
    }

    fun testNestedConditionDoesNotExcludeOuterConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        addKotlin(
            "OuterConfig",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            @Configuration
            class OuterConfig {
                @ConditionalOnProperty(name = ["x.enabled"], havingValue = "true")
                class Nested
            }
            """
        )

        assertActive("com.app.OuterConfig", "x.enabled")
    }
}
