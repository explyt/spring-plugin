/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    @TestMetadata("yaml")
    fun testYaml() = doTest(SpringYamlInspection())

    fun testDuplicateProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <error>barBaz</error>: some1
    <error>bar-Baz</error>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testKebabCaseProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <weak_warning descr="Key is not in Spring's canonical form">barBaz</weak_warning>: some1
    <weak_warning descr="Key is not in Spring's canonical form">bar-Baz1</weak_warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testFileDefinitionProperties() {
        @Language("java") val configurationProperty = """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.prop")
            public class ConfigProperties {               
                private String test;                           
                public void setTest(String test) { this.test = test}
                public String getTest() { return this.test}
            } 
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.prop:
    test: some1
    <warning descr="Cannot resolve key property 'explyt.prop.field'">field</warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testFileDefinitionPropertiesSubstring() {
        @Language("java") val configurationProperty = """
            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt-prop")
            public class ConfigProperties {               
                private String test;                           
                public void setTest(String test) { this.test = test}
                public String getTest() { return this.test}
            } 
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.prop:
    test: some1
explyt: some2
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testListElementUnderDigitBoundaryKey() {
        @Language("java") val configurationProperty = """
            import java.util.List;

            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.digit")
            public class S3ConfigProperties {
                private S3Logs s3Logs = new S3Logs();
                public S3Logs getS3Logs() { return s3Logs; }
                public void setS3Logs(S3Logs s3Logs) { this.s3Logs = s3Logs; }

                public static class S3Logs {
                    private List<Source> sources;
                    public List<Source> getSources() { return sources; }
                    public void setSources(List<Source> sources) { this.sources = sources; }
                }

                public static class Source {
                    private String name;
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
            }
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.digit:
  s3-logs:
    sources:
      - name: first
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testListElementUnderNonKebabKey() {
        @Language("java") val configurationProperty = """
            import java.util.List;

            @org.springframework.context.annotation.Configuration
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.camel")
            public class CamelConfigProperties {
                private Holder camelWritten = new Holder();
                public Holder getCamelWritten() { return camelWritten; }
                public void setCamelWritten(Holder camelWritten) { this.camelWritten = camelWritten; }

                public static class Holder {
                    private List<Item> items;
                    public List<Item> getItems() { return items; }
                    public void setItems(List<Item> items) { this.items = items; }
                }

                public static class Item {
                    private String name;
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
            }
        """.trimIndent()
        myFixture.addClass(configurationProperty)
        myFixture.configureByText(
            "application.yaml",
            """
explyt.camel:
  <weak_warning descr="Key is not in Spring's canonical form">camelWritten</weak_warning>:
    items:
      - name: first
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    /**
     * A property that only ever appears in a `@Value` placeholder with a SpEL default
     * (`:#{null}`) is still a real, resolvable property. The nested brace group used to break
     * placeholder key extraction, so no reference was created and the key was reported as
     * unresolved even though Spring injects it at runtime.
     */
    fun testKeyUsedOnlyInValueWithSpelDefault() {
        @Language("java") val knownProperties = """
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.placeholder")
            public class PlaceholderKnownProperties {
                private String known;
                public String getKnown() { return known; }
                public void setKnown(String known) { this.known = known; }
            }
        """.trimIndent()
        myFixture.addClass(knownProperties)

        @Language("java") val consumer = """
            import org.springframework.beans.factory.annotation.Value;

            @org.springframework.stereotype.Component
            public class PlaceholderConsumer {
                @Value("${'$'}{explyt.placeholder.spel-default:#{null}}")
                private String spelDefault;

                @Value("${'$'}{explyt.placeholder.plain-default:someDefault}")
                private String plainDefault;
            }
        """.trimIndent()
        myFixture.addClass(consumer)

        myFixture.configureByText(
            "application.yaml",
            """
explyt.placeholder:
  known: someValue
  spel-default: someValue
  plain-default: someValue
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testComplexKebabCaseProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    barbaz.<weak_warning descr="Key is not in Spring's canonical form">testProp</weak_warning>: some1    
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testKebabCaseQuickFix() {
        myFixture.configureByText(
            "application.yaml",
            """
fooFoo:
  bar:
    baz: 5   
            """.trimIndent()
        )
        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
foo-foo:
  bar:
    baz: 5 
        """.trimIndent(), true
        )
    }

    fun testKebabCaseQuickFixMiddle() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
  barFoo:
    baz: 5   
            """.trimIndent()
        )
        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
foo:
  bar-foo:
    baz: 5 
        """.trimIndent(), true
        )
    }

    fun testKebabCaseQuickFixEnd() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
  bar:
    bazFoo: 5   
            """.trimIndent()
        )
        val quickFix = myFixture.getAllQuickFixes().firstOrNull()
        assertNotNull(quickFix)
        myFixture.launchAction(quickFix!!)
        myFixture.checkResult(
            """
foo:
  bar:
    baz-foo: 5 
        """.trimIndent(), true
        )
    }

    /**
     * A key whose value is a sequence is a leaf property like any other. It used to be collected only when its
     * value was a scalar, so every per-key check skipped it silently.
     */
    fun testSequenceKeyInNonCanonicalFormIsReported() {
        myFixture.addClass(listConfigurationProperties("pathsToExclude"))
        myFixture.configureByText(
            "application.yaml",
            """
explyt.doc:
  <weak_warning descr="Key is not in Spring's canonical form">paths_to_exclude</weak_warning>:
    - /actuator/**
    - /internal/**
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testSequenceKeyThatDoesNotResolveIsReported() {
        myFixture.addClass(listConfigurationProperties("pathsToExclude"))
        myFixture.configureByText(
            "application.yaml",
            """
explyt.doc:
  <warning descr="Cannot resolve key property 'explyt.doc.unknown-list'">unknown-list</warning>:
    - /actuator/**
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    /** The regression guard for over-reporting: a resolvable list key must stay clean. */
    fun testResolvableSequenceKeyIsNotReported() {
        myFixture.addClass(listConfigurationProperties("pathsToExclude"))
        myFixture.configureByText(
            "application.yaml",
            """
explyt.doc:
  paths-to-exclude:
    - /actuator/**
    - /internal/**
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    /**
     * The value checks run on a scalar value. A sequence has no scalar value — `YAMLKeyValue.getValueText` answers a
     * synthetic `<sequence:…>` marker — so a class-reference key must report nothing rather than the marker.
     */
    fun testClassReferenceCheckDoesNotFireOnASequence() {
        myFixture.configureByText(
            "application.yaml",
            """
spring:
  main:
    sources:
      - com.explyt.NotAClassButAListElement
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    @Language("java")
    private fun listConfigurationProperties(propertyName: String) = """
        import java.util.List;

        @org.springframework.context.annotation.Configuration
        @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.doc")
        public class DocConfigProperties {
            private List<String> $propertyName;
            public List<String> get${propertyName.replaceFirstChar { it.uppercase() }}() { return $propertyName; }
            public void set${propertyName.replaceFirstChar { it.uppercase() }}(List<String> $propertyName) {
                this.$propertyName = $propertyName;
            }
        }
    """.trimIndent()
}
