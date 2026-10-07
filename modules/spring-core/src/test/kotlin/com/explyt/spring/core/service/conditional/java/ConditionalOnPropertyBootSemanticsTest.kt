/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.ConditionalOnPropertyBootSemanticsTestCase

class ConditionalOnPropertyBootSemanticsTest : ConditionalOnPropertyBootSemanticsTestCase() {

    override fun setUp() {
        super.setUp()
        myFixture.addFileToProject(
            "com/app/Application.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class Application {}
            """.trimIndent()
        )
    }

    fun testRepeatedAnnotationsMustAllMatch() {
        addProperties("application.properties", "app.first.enabled=true", "app.second.enabled=false")
        addConfiguration(
            "RepeatedConfig",
            """
            @ConditionalOnProperty(name = "app.first.enabled", havingValue = "true")
            @ConditionalOnProperty(name = "app.second.enabled", havingValue = "true")
            """
        )

        assertInactive("com.app.RepeatedConfig", "app.first.enabled", "app.second.enabled")
    }

    fun testEveryNameMustMatch() {
        addProperties("application.properties", "app.first.enabled=true", "app.second.enabled=false")
        addConfiguration(
            "AllNamesConfig",
            """@ConditionalOnProperty(name = {"app.first.enabled", "app.second.enabled"}, havingValue = "true")"""
        )

        assertInactive("com.app.AllNamesConfig", "app.first.enabled", "app.second.enabled")
    }

    fun testHavingValueIsComparedIgnoringCase() {
        addProperties("application.properties", "app.feature.enabled=true")
        addConfiguration("UpperCaseConfig", """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "TRUE")""")

        assertActive("com.app.UpperCaseConfig", "app.feature.enabled")
    }

    fun testFalseValueWithoutHavingValueDoesNotMatch() {
        addProperties("application.properties", "app.feature.enabled=false")
        addConfiguration("FalseFlagConfig", """@ConditionalOnProperty("app.feature.enabled")""")

        assertInactive("com.app.FalseFlagConfig", "app.feature.enabled")
    }

    fun testAnyValueOtherThanFalseWithoutHavingValueMatches() {
        addProperties("application.properties", "app.feature.mode=fast")
        addConfiguration("ModeConfig", """@ConditionalOnProperty("app.feature.mode")""")

        assertActive("com.app.ModeConfig", "app.feature.mode")
    }

    fun testMissingKeyWithMatchIfMissingMatches() {
        addProperties("application.properties", "app.marker=present")
        addConfiguration("MissingConfig", """@ConditionalOnProperty(name = "app.feature.enabled", matchIfMissing = true)""")

        assertActive("com.app.MissingConfig", "app.marker")
    }

    fun testMissingKeyWithHavingValueAndMatchIfMissingMatches() {
        addProperties("application.properties", "app.marker=present")
        addConfiguration(
            "MissingWithValueConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true", matchIfMissing = true)"""
        )

        assertActive("com.app.MissingWithValueConfig", "app.marker")
    }

    fun testMissingKeyWithoutMatchIfMissingDoesNotMatch() {
        addProperties("application.properties", "app.marker=present")
        addConfiguration("AbsentConfig", """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")""")

        assertInactive("com.app.AbsentConfig", "app.marker")
    }

    fun testPrefixWithoutTrailingDotIsJoinedWithDot() {
        addProperties("application.properties", "app.feature.enabled=true")
        addConfiguration(
            "PrefixConfig",
            """@ConditionalOnProperty(prefix = "app.feature", name = "enabled", havingValue = "true")"""
        )

        assertActive("com.app.PrefixConfig", "app.feature.enabled")
    }

    fun testActiveProfileFileOverridesDefaultFile() {
        addProperties("application.properties", "spring.profiles.active=prod", "app.feature.enabled=false")
        addProperties("application-prod.properties", "app.feature.enabled=true")
        addConfiguration("ProdProfileConfig", """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")""")

        assertActive("com.app.ProdProfileConfig", "app.feature.enabled")
    }

    fun testInactiveProfileFileIsIgnored() {
        addProperties("application.properties", "spring.profiles.active=dev", "app.feature.enabled=false")
        addProperties("application-prod.properties", "app.feature.enabled=true")
        addConfiguration("DevProfileConfig", """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")""")

        assertInactive("com.app.DevProfileConfig", "app.feature.enabled")
    }

    fun testPlaceholderDefaultIsUsedWhenTheVariableIsUnset() {
        addProperties("application.properties", "app.feature.enabled=\${EXPLYT_UNSET_FEATURE_FLAG:true}")
        addConfiguration(
            "PlaceholderTrueConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")"""
        )

        assertActive("com.app.PlaceholderTrueConfig", "app.feature.enabled")
    }

    fun testPlaceholderFalseDefaultWithoutHavingValueDoesNotMatch() {
        addProperties("application.properties", "app.feature.enabled=\${EXPLYT_UNSET_FEATURE_FLAG:false}")
        addConfiguration("PlaceholderFalseConfig", """@ConditionalOnProperty("app.feature.enabled")""")

        assertInactive("com.app.PlaceholderFalseConfig", "app.feature.enabled")
    }

    fun testBooleanPropertyFalseDoesNotMatch() {
        addProperties("application.properties", "app.feature.enabled=false")
        addConfiguration("BooleanFalseConfig", """@ConditionalOnBooleanProperty("app.feature.enabled")""")

        assertInactive("com.app.BooleanFalseConfig", "app.feature.enabled")
    }

    fun testBooleanPropertyTrueMatches() {
        addProperties("application.properties", "app.feature.enabled=true")
        addConfiguration("BooleanTrueConfig", """@ConditionalOnBooleanProperty("app.feature.enabled")""")

        assertActive("com.app.BooleanTrueConfig", "app.feature.enabled")
    }

    private fun addConfiguration(className: String, conditions: String) {
        myFixture.addFileToProject(
            "com/app/$className.java",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            ${conditions.trimIndent()}
            public class $className {}
            """.trimIndent()
        )
    }
}
