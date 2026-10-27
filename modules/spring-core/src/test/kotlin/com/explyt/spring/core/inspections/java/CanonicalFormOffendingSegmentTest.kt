/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.openapi.editor.Document
import org.intellij.lang.annotations.Language

/**
 * The canonical-form problem has to be reported on the segment that actually deviates. The inspection tests the
 * full key, so before this behaviour existed a perfectly canonical leaf was underlined because an ancestor was
 * camelCase — `explyt.camel.camelWritten.items[0].name` reported on `name`.
 *
 * These tests read the highlight offsets back from the document instead of relying on marker placement, so they
 * pin *which* text is underlined rather than only that something is.
 */
class CanonicalFormOffendingSegmentTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBoot_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java, SpringPropertiesInspection::class.java)
    }

    fun testYamlReportsTheAncestorAndLeavesTheCanonicalLeafClean() {
        addCamelConfigurationProperties()
        myFixture.configureByText(
            "application.yaml",
            """
            explyt.camel:
              camelWritten:
                items:
                  - name: first
            """.trimIndent()
        )

        val underlined = canonicalFormHighlights()

        assertEquals("Exactly one canonical-form problem is expected, got $underlined", 1, underlined.size)
        assertEquals("camelWritten", underlined.single())
    }

    /**
     * Several leaves under one non-canonical ancestor all resolve to that ancestor, so the problem must be reported
     * once rather than stacked per leaf. The precondition is asserted first: a fixture that produced no problem at
     * all would satisfy "at most one" silently.
     */
    fun testSeveralLeavesUnderOneAncestorProduceOneProblem() {
        addCamelConfigurationProperties()
        myFixture.configureByText(
            "application.yaml",
            """
            explyt.camel:
              camelWritten:
                items:
                  - name: first
                  - name: second
                  - name: third
            """.trimIndent()
        )

        val underlined = canonicalFormHighlights()

        assertTrue("The fixture must produce a canonical-form problem, otherwise the count proves nothing",
            underlined.isNotEmpty())
        assertEquals("One ancestor must yield one problem, got $underlined", 1, underlined.size)
        assertEquals("camelWritten", underlined.single())
    }

    /** A single YAML element can hold several dotted segments, so the range is narrowed inside the element too. */
    fun testYamlNarrowsTheRangeInsideACompoundKey() {
        myFixture.configureByText(
            "application.yaml",
            """
            foo:
              barbaz.testProp: some1
            """.trimIndent()
        )

        val underlined = canonicalFormHighlights()

        assertEquals("Exactly one canonical-form problem is expected, got $underlined", 1, underlined.size)
        assertEquals("testProp", underlined.single())
    }

    fun testPropertiesNarrowsTheRangeToTheOffendingSegment() {
        myFixture.configureByText("application.properties", "test.fooBar=1")

        val underlined = canonicalFormHighlights()

        assertEquals("Exactly one canonical-form problem is expected, got $underlined", 1, underlined.size)
        assertEquals("fooBar", underlined.single())
    }

    /** A map entry name is arbitrary, so it stays exempt — the whole key is non-canonical yet nothing is reported. */
    fun testMapEntryNameStaysExempt() {
        @Language("java") val mapProperties = """
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.labels")
            public class LabelProperties {
                private java.util.Map<String, String> values;
                public java.util.Map<String, String> getValues() { return values; }
                public void setValues(java.util.Map<String, String> values) { this.values = values; }
            }
        """.trimIndent()
        myFixture.addClass(mapProperties)
        myFixture.configureByText(
            "application.yaml",
            """
            explyt.labels:
              values:
                myEntryName: some1
            """.trimIndent()
        )

        assertEquals("A map entry name must stay exempt", emptyList<String>(), canonicalFormHighlights())
    }

    private fun addCamelConfigurationProperties() {
        @Language("java") val configurationProperty = """
            @org.springframework.boot.context.properties.ConfigurationProperties(prefix = "explyt.camel")
            public class CamelConfigProperties {
                private Holder camelWritten;
                public Holder getCamelWritten() { return camelWritten; }
                public void setCamelWritten(Holder camelWritten) { this.camelWritten = camelWritten; }

                public static class Holder {
                    private java.util.List<Item> items;
                    public java.util.List<Item> getItems() { return items; }
                    public void setItems(java.util.List<Item> items) { this.items = items; }
                }

                public static class Item {
                    private String name;
                    public String getName() { return name; }
                    public void setName(String name) { this.name = name; }
                }
            }
        """.trimIndent()
        myFixture.addClass(configurationProperty)
    }

    /** The exact text each canonical-form problem underlines, read back from the document. */
    private fun canonicalFormHighlights(): List<String> {
        val message = SpringCoreBundle.message("explyt.spring.inspection.properties.value.should.be.kebab")
        val document: Document = myFixture.editor.document
        return myFixture.doHighlighting()
            .filter { it.description == message }
            .onEach {
                assertEquals(
                    "The canonical-form problem must stay a weak warning",
                    HighlightSeverity.WEAK_WARNING,
                    it.severity
                )
            }
            .map { document.getText(com.intellij.openapi.util.TextRange(it.startOffset, it.endOffset)) }
    }
}
