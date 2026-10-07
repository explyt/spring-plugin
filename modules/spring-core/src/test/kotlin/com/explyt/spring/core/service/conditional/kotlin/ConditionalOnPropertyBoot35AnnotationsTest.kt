/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.test.TestLibrary

class ConditionalOnPropertyBoot35AnnotationsTest : KotlinPropertyConditionTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)

    override val conditionAnnotations: List<String> = listOf(
        "ConditionalOnProperty",
        "ConditionalOnProperties",
        "ConditionalOnBooleanProperty",
        "ConditionalOnBooleanProperties"
    )

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

    fun testBooleanPropertyFalseDoesNotMatch() {
        addProperties("application.yaml", "app:", "  feature:", "    enabled: false")
        addConfiguration("BooleanFalseConfig", """@ConditionalOnBooleanProperty("app.feature.enabled")""")

        assertInactive("com.app.BooleanFalseConfig", "app.feature.enabled")
    }

    fun testExplicitPropertyContainerRequiresEveryAnnotationToMatch() {
        addProperties("application.properties", "app.first.enabled=true", "app.second.enabled=false")
        addConfiguration(
            "ExplicitPropertyContainerConfig",
            """
            @ConditionalOnProperties(
                ConditionalOnProperty(name = ["app.first.enabled"], havingValue = "true"),
                ConditionalOnProperty(name = ["app.second.enabled"], havingValue = "true")
            )
            """
        )

        assertInactive("com.app.ExplicitPropertyContainerConfig", "app.first.enabled", "app.second.enabled")
    }

    fun testExplicitBooleanPropertyContainerRequiresEveryAnnotationToMatch() {
        addProperties("application.properties", "app.first.enabled=true", "app.second.enabled=false")
        addConfiguration(
            "ExplicitBooleanContainerConfig",
            """
            @ConditionalOnBooleanProperties(
                ConditionalOnBooleanProperty(name = ["app.first.enabled"]),
                ConditionalOnBooleanProperty(name = ["app.second.enabled"])
            )
            """
        )

        assertInactive("com.app.ExplicitBooleanContainerConfig", "app.first.enabled", "app.second.enabled")
    }
}
