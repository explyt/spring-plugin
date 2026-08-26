/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference

/**
 * `management.endpoint.<id>.*` is formatted from the endpoint id at runtime, so the keys of an endpoint the project
 * declares itself are valid although no metadata declares them.
 */
class ActuatorEndpointKeyTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java, SpringYamlInspection::class.java)
    }

    fun testCustomEndpointKeysAreNotReported() {
        addCustomEndpoint()

        myFixture.configureByText(
            "application.properties",
            """
            management.endpoint.outboxpublishers.access=unrestricted
            management.endpoint.outboxpublishers.enabled=true
            management.endpoint.outboxpublishers.cache.time-to-live=10s
            """.trimIndent()
        )

        assertEquals("expected no unresolved key, got: ${unresolvedKeys()}", emptyList<String>(), unresolvedKeys())
    }

    fun testYamlCustomEndpointKeysAreNotReported() {
        addCustomEndpoint()

        myFixture.configureByText(
            "application.yaml",
            """
            management:
              endpoint:
                outboxpublishers:
                  access: unrestricted
                  enabled: true
                  cache:
                    time-to-live: 10s
            """.trimIndent()
        )

        assertEquals("expected no unresolved key, got: ${unresolvedKeys()}", emptyList<String>(), unresolvedKeys())
    }

    fun testUnknownEndpointIdIsStillReported() {
        addCustomEndpoint()

        myFixture.configureByText("application.properties", "management.endpoint.nosuchendpoint.access=unrestricted")

        assertEquals("an id no endpoint declares stays unresolved", 1, unresolvedKeys().size)
    }

    fun testEndpointWithoutIdProducesNoKeys() {
        myFixture.addClass(
            """
            import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
            @Endpoint
            public class NoIdEndpoint {}
            """.trimIndent()
        )

        myFixture.configureByText("application.properties", "management.endpoint..access=unrestricted")

        assertEquals("an empty id must synthesize nothing", 1, unresolvedKeys().size)
    }

    fun testCompletionOffersSynthesizedKeys() {
        addCustomEndpoint()

        myFixture.configureByText("application.properties", "management.endpoint.outboxpublishers.<caret>")
        myFixture.completeBasic()

        val variants = myFixture.lookupElementStrings.orEmpty()
        assertTrue(
            "expected the synthesized keys among the variants, got: $variants",
            variants.any { it.contains("outboxpublishers.access") }
                    && variants.any { it.contains("outboxpublishers.enabled") }
                    && variants.any { it.contains("outboxpublishers.cache.time-to-live") }
        )
    }

    fun testIdSegmentNavigatesToItsEndpointClass() {
        addCustomEndpoint()
        myFixture.configureByText("application.properties", "management.endpoint.outboxpublishers.access=unrestricted")

        val targets = targetsAt("outboxpublishers")

        assertEquals("the id names one endpoint, got: ${describe(targets)}", 1, targets.size)
        assertEquals("OutboxPublishersEndpoint", (targets.single() as? PsiClass)?.name)
    }

    fun testAccessSegmentNavigatesToValueType() = assertTailNavigatesTo("access", "Access")

    fun testEnabledSegmentNavigatesToValueType() = assertTailNavigatesTo("enabled", "Boolean")

    fun testCacheTailSegmentNavigatesToValueType() = assertTailNavigatesTo("cache.time-to-live", "Duration")

    fun testYamlIdSegmentNavigatesToItsEndpointClass() {
        addCustomEndpoint()
        myFixture.configureByText(
            "application.yaml",
            """
            management:
              endpoint:
                outboxpublishers:
                  access: unrestricted
            """.trimIndent()
        )

        val targets = targetsAt("outboxpublishers")

        assertEquals("the id names one endpoint, got: ${describe(targets)}", 1, targets.size)
        assertEquals("OutboxPublishersEndpoint", (targets.single() as? PsiClass)?.name)
    }

    fun testBuiltInEndpointIdKeepsOneTargetPerSegment() {
        myFixture.configureByText("application.properties", "management.endpoint.env.access=unrestricted")

        val idTargets = targetsAt("env")
        assertTrue(
            "a built-in id has no declaring class in the project, got: ${describe(idTargets)}",
            idTargets.size <= 1
        )
        val accessTargets = targetsAtLast("access")
        assertEquals("one target for the value segment, got: ${describe(accessTargets)}", 1, accessTargets.size)
    }

    private fun assertTailNavigatesTo(tail: String, expectedType: String) {
        addCustomEndpoint()
        myFixture.configureByText("application.properties", "management.endpoint.outboxpublishers.$tail=")

        val targets = targetsAtLast(tail.substringAfterLast('.'))

        assertEquals("one value type per key, got: ${describe(targets)}", 1, targets.size)
        assertEquals(expectedType, (targets.single() as? PsiClass)?.name)
    }

    private fun addCustomEndpoint() {
        myFixture.addClass(
            """
            import org.springframework.boot.actuate.endpoint.Access;
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint;

            @WebEndpoint(id = "outboxpublishers", defaultAccess = Access.NONE)
            public class OutboxPublishersEndpoint {
                @ReadOperation
                public String status() { return "ok"; }
            }
            """.trimIndent()
        )
    }

    private fun unresolvedKeys(): List<String> = myFixture.doHighlighting().asSequence()
        .mapNotNull { it.description }
        .filter { it.contains("Cannot resolve key property") }
        .toList()

    private fun targetsAt(segment: String): List<PsiElement> =
        resolveAt(myFixture.file.text.indexOf(segment) + segment.length - 1)

    private fun targetsAtLast(segment: String): List<PsiElement> =
        resolveAt(myFixture.file.text.lastIndexOf(segment) + segment.length - 1)

    private fun resolveAt(offset: Int): List<PsiElement> {
        val reference = myFixture.file.findReferenceAt(offset) ?: return emptyList()
        return when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference.resolve())
        }
    }

    private fun describe(targets: List<PsiElement>) =
        targets.map { "${it::class.java.simpleName}[${it.text?.take(40)}]" }
}
