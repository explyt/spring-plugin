/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

class ConditionalOnPropertyBootSemanticsTest : JavaPropertyConditionTestCase() {

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

    fun testActiveProfileDocumentOverridesDefaultDocument() {
        addProperties("application.yaml", *profileDocuments("prod"))
        addConfiguration("ProdDocumentConfig", """@ConditionalOnProperty(name = "x.enabled", havingValue = "true")""")

        assertActive("com.app.ProdDocumentConfig", "x.enabled")
    }

    fun testInactiveProfileDocumentIsIgnored() {
        addProperties("application.yaml", *profileDocuments("dev"))
        addConfiguration("DevDocumentConfig", """@ConditionalOnProperty(name = "x.enabled", havingValue = "true")""")

        assertInactive("com.app.DevDocumentConfig", "x.enabled")
    }

    fun testKeyOnlyInInactiveProfileDocumentIsMissing() {
        addProperties(
            "application.yaml",
            "spring:", "  profiles:", "    active: dev",
            "---",
            "spring:", "  config:", "    activate:", "      on-profile: prod",
            "x:", "  only: true"
        )
        addConfiguration(
            "InactiveDocumentConfig",
            """@ConditionalOnProperty(name = "x.only", havingValue = "false", matchIfMissing = true)"""
        )

        assertActive("com.app.InactiveDocumentConfig", "x.only")
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

    fun testPlaceholderUsesDefinedPropertyValueBeforeDefault() {
        addProperties(
            "application.properties",
            "app.source.enabled=true",
            "app.feature.enabled=\${app.source.enabled:false}"
        )
        addConfiguration(
            "DefinedPlaceholderConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")"""
        )

        assertActive("com.app.DefinedPlaceholderConfig", "app.feature.enabled", "app.source.enabled")
    }

    fun testNestedPlaceholderDefaultIsResolved() {
        addProperties("application.properties", "app.feature.enabled=\${EXPLYT_UNSET_A:\${EXPLYT_UNSET_B:true}}")
        addConfiguration(
            "NestedDefaultConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")"""
        )

        assertActive("com.app.NestedDefaultConfig", "app.feature.enabled")
    }

    fun testUnresolvablePlaceholderKeepsTheBean() {
        addProperties("application.properties", "app.feature.enabled=\${EXPLYT_UNSET_FEATURE_FLAG}")
        addConfiguration(
            "UnresolvableConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")"""
        )

        assertActive("com.app.UnresolvableConfig", "app.feature.enabled")
    }

    fun testUnresolvablePlaceholderWithoutHavingValueKeepsTheBean() {
        addProperties("application.properties", "app.feature.enabled=\${EXPLYT_UNSET_FEATURE_FLAG}")
        addConfiguration("UnresolvableFlagConfig", """@ConditionalOnProperty("app.feature.enabled")""")

        assertActive("com.app.UnresolvableFlagConfig", "app.feature.enabled")
    }

    fun testEmbeddedUnresolvablePlaceholderKeepsTheBean() {
        addProperties("application.properties", "app.feature.mode=fast-\${EXPLYT_UNSET_FEATURE_FLAG}")
        addConfiguration(
            "EmbeddedPlaceholderConfig",
            """@ConditionalOnProperty(name = "app.feature.mode", havingValue = "fast-on")"""
        )

        assertActive("com.app.EmbeddedPlaceholderConfig", "app.feature.mode")
    }

    fun testNestedUnresolvablePlaceholderKeepsTheBean() {
        addProperties("application.properties", "app.feature.enabled=\${EXPLYT_UNSET_A:\${EXPLYT_UNSET_B}}")
        addConfiguration(
            "NestedUnresolvableConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")"""
        )

        assertActive("com.app.NestedUnresolvableConfig", "app.feature.enabled")
    }

    fun testReferencedUnresolvablePlaceholderKeepsTheBean() {
        addProperties(
            "application.properties",
            "app.source.enabled=\${EXPLYT_UNSET_FEATURE_FLAG}",
            "app.feature.enabled=\${app.source.enabled:false}"
        )
        addConfiguration(
            "ReferencedUnresolvableConfig",
            """@ConditionalOnProperty(name = "app.feature.enabled", havingValue = "true")"""
        )

        assertActive("com.app.ReferencedUnresolvableConfig", "app.feature.enabled", "app.source.enabled")
    }

    fun testMetaAnnotationCarriesPropertyCondition() {
        addProperties("application.properties", "app.meta.enabled=false")
        addJava(
            "MetaConfig",
            """
            package com.app;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @ConditionalOnProperty(name = "app.meta.enabled", havingValue = "true")
            @interface EnabledWhenProperty {}

            @Configuration
            @EnabledWhenProperty
            public class MetaConfig {}
            """
        )

        assertInactive("com.app.MetaConfig", "app.meta.enabled")
    }

    fun testMetaAnnotationMatchIfMissingIsRead() {
        addProperties("application.properties", "app.marker=present")
        addJava(
            "MetaMissingConfig",
            """
            package com.app;

            import java.lang.annotation.ElementType;
            import java.lang.annotation.Retention;
            import java.lang.annotation.RetentionPolicy;
            import java.lang.annotation.Target;
            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Target(ElementType.TYPE)
            @Retention(RetentionPolicy.RUNTIME)
            @ConditionalOnProperty(name = "x", matchIfMissing = true)
            @interface EnabledWhen {}

            @Configuration
            @EnabledWhen
            public class MetaMissingConfig {}
            """
        )

        assertActive("com.app.MetaMissingConfig", "app.marker")
    }

    fun testMethodConditionDoesNotExcludeConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        addJava(
            "MethodConfig",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class MethodConfig {
                @Bean
                @ConditionalOnProperty(name = "x.enabled", havingValue = "true")
                public String disabledBean() { return "disabled"; }
            }
            """
        )

        assertActive("com.app.MethodConfig", "x.enabled")
        assertBeanMethodExcluded("disabledBean")
    }

    fun testNestedConditionDoesNotExcludeOuterConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        addJava(
            "OuterConfig",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class OuterConfig {
                @ConditionalOnProperty(name = "x.enabled", havingValue = "true")
                static class Nested {}
            }
            """
        )

        assertActive("com.app.OuterConfig", "x.enabled")
    }

    fun testLocalClassConditionDoesNotExcludeConfiguration() {
        addProperties("application.properties", "x.enabled=false")
        addJava(
            "LocalClassConfig",
            """
            package com.app;

            import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
            import org.springframework.context.annotation.Configuration;

            @Configuration
            public class LocalClassConfig {
                void build() {
                    @ConditionalOnProperty(name = "x.enabled", havingValue = "true")
                    class Local {}
                }
            }
            """
        )

        assertActive("com.app.LocalClassConfig", "x.enabled")
    }

    fun testEscapedPlaceholderIsLiteral() {
        addProperties("application.properties", "x.enabled=\\\\${'$'}{A}")
        addConfiguration("EscapedLiteralConfig", """@ConditionalOnProperty(name = "x.enabled", havingValue = "${'$'}{A}")""")
        assertActive("com.app.EscapedLiteralConfig", "x.enabled")
    }

    fun testEscapedPlaceholderDoesNotMatchTrue() {
        addProperties("application.properties", "x.enabled=\\\\${'$'}{A}")
        addConfiguration("EscapedPlaceholderConfig", """@ConditionalOnProperty(name = "x.enabled", havingValue = "true")""")
        assertInactive("com.app.EscapedPlaceholderConfig", "x.enabled")
    }

    fun testActivePropertiesDocumentOverridesDefaultDocument() {
        addProperties("application.properties", "x.enabled=false", "#---", "spring.config.activate.on-profile=prod", "x.enabled=true")
        addProperties("application-prod.properties", "spring.profiles.active=prod")
        addConfiguration("PropertiesDocumentConfig", """@ConditionalOnProperty(name = "x.enabled", havingValue = "true")""")
        assertActive("com.app.PropertiesDocumentConfig", "x.enabled")
    }

    fun testInactivePropertiesDocumentIsIgnored() {
        addProperties("application.properties", "spring.profiles.active=dev", "x.enabled=false", "#---", "spring.config.activate.on-profile=prod", "x.enabled=true")
        addConfiguration("InactivePropertiesDocumentConfig", """@ConditionalOnProperty(name = "x.enabled", havingValue = "true")""")
        assertInactive("com.app.InactivePropertiesDocumentConfig", "x.enabled")
    }

    fun testKeyOnlyInInactivePropertiesDocumentIsMissing() {
        addProperties("application.properties", "spring.profiles.active=dev", "#---", "spring.config.activate.on-profile=prod", "x.only=true")
        addConfiguration("InactivePropertiesMissingConfig", """@ConditionalOnProperty(name = "x.only", havingValue = "false", matchIfMissing = true)""")
        assertActive("com.app.InactivePropertiesMissingConfig", "x.only")
    }

    private fun profileDocuments(activeProfile: String): Array<String> = arrayOf(
        "spring:", "  profiles:", "    active: $activeProfile",
        "x:", "  enabled: false",
        "---",
        "spring:", "  config:", "    activate:", "      on-profile: prod",
        "x:", "  enabled: true"
    )
}
