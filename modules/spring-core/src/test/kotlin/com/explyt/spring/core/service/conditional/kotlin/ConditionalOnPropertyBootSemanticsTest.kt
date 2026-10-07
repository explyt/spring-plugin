/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.ConditionalOnPropertyBootSemanticsTestCase

class ConditionalOnPropertyBootSemanticsTest : ConditionalOnPropertyBootSemanticsTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/app/Application.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.SpringBootApplication

            @SpringBootApplication
            class Application
            """.trimIndent()
        )
    }

    fun testRepeatedAnnotationsMustAllMatch() {
        addProperties("application.properties", "app.first.enabled=true", "app.second.enabled=false")
        addConfiguration(
            "RepeatedConfig",
            """
            @ConditionalOnProperty(name = ["app.first.enabled"], havingValue = "true")
            @ConditionalOnProperty(name = ["app.second.enabled"], havingValue = "true")
            """
        )

        assertInactive("com.app.RepeatedConfig", "app.first.enabled", "app.second.enabled")
    }

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

    fun testPlaceholderFalseDefaultMatchesHavingValueFalse() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: \${EXPLYT_UNSET_FEATURE_FLAG:false}")
        addConfiguration(
            "PlaceholderFalseConfig",
            """@ConditionalOnProperty(name = ["app.feature.enabled"], havingValue = "false")"""
        )

        assertActive("com.app.PlaceholderFalseConfig", "app.feature.enabled")
    }

    fun testBooleanPropertyFalseDoesNotMatch() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: false")
        addConfiguration("BooleanFalseConfig", """@ConditionalOnBooleanProperty("app.feature.enabled")""")

        assertInactive("com.app.BooleanFalseConfig", "app.feature.enabled")
    }

    fun testMethodConditionDoesNotExcludeConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        myFixture.addFileToProject("com/app/MethodConfig.kt", """
            package com.app
            import org.springframework.context.annotation.Configuration
            import org.springframework.context.annotation.Bean
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            @Configuration
            class MethodConfig {
                @Bean
                @ConditionalOnProperty(name = ["x.enabled"], havingValue = "true")
                fun disabledBean(): String = "disabled"
            }
            """.trimIndent())
        assertActive("com.app.MethodConfig", "x.enabled")
        val facade = com.explyt.spring.core.service.SpringSearchServiceFacade.getInstance(project)
        assertFalse(facade.getAllActiveBeans(module).any { it.psiMember.name == "disabledBean" })
        assertTrue(facade.getExcludedBeansClasses(module).any { it.psiMember.name == "disabledBean" })
    }

    fun testNestedConditionDoesNotExcludeOuterConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        myFixture.addFileToProject("com/app/OuterConfig.kt", """
            package com.app
            import org.springframework.context.annotation.Configuration
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            @Configuration
            class OuterConfig {
                @ConditionalOnProperty(name = ["x.enabled"], havingValue = "true")
                class Nested
            }
            """.trimIndent())
        assertActive("com.app.OuterConfig", "x.enabled")
    }

    private fun addConfiguration(className: String, conditions: String) {
        myFixture.addFileToProject(
            "com/app/$className.kt",
            """
            package com.app

            import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
            import org.springframework.context.annotation.Configuration

            @Configuration
            ${conditions.trimIndent()}
            class $className
            """.trimIndent()
        )
    }
}
