/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.core.inspections.SpringPropertiesInspection
import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiPolyVariantReference

/**
 * The reported case is Kotlin, where the endpoint is an `internal class` and the annotation search sees a light class;
 * discovery must find it exactly as it finds a Java one.
 */
class ActuatorEndpointKotlinKeyTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringPropertiesInspection::class.java, SpringYamlInspection::class.java)
    }

    fun testInternalKotlinEndpointKeysAreNotReported() {
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

    fun testYamlInternalKotlinEndpointKeysAreNotReported() {
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

        assertEquals("expected no unresolved key, got: ${unresolvedKeys()}", emptyList<String>(), unresolvedKeys())
    }

    fun testYamlCacheTailNavigatesToDuration() {
        addCustomEndpoint()
        myFixture.configureByText(
            "application.yaml",
            """
            management:
              endpoint:
                outboxpublishers:
                  cache:
                    time-to-live: 10s
            """.trimIndent()
        )

        val reference = myFixture.file.findReferenceAt(myFixture.file.text.indexOf("time-to-live"))
        val targets = when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference?.resolve())
        }

        assertEquals("one value type per YAML key, got: ${targets.map { describe(it) }}", 1, targets.size)
        assertEquals("Duration", (targets.single() as? PsiClass)?.name)
    }

    fun testIdSegmentNavigatesToTheKotlinEndpointClass() {
        addCustomEndpoint()
        myFixture.configureByText("application.properties", "management.endpoint.outboxpublishers.access=unrestricted")

        val reference = myFixture.file.findReferenceAt(
            myFixture.file.text.indexOf("outboxpublishers") + "outboxpublishers".length - 1
        )
        val targets = when (reference) {
            is PsiPolyVariantReference -> reference.multiResolve(false).mapNotNull { it.element }
            else -> listOfNotNull(reference?.resolve())
        }

        assertEquals("the id names one endpoint, got: ${targets.map { describe(it) }}", 1, targets.size)
        assertEquals("OutboxPublishersEndpoint", (targets.single() as? PsiClass)?.name)
    }

    private fun addCustomEndpoint() {
        myFixture.addFileToProject(
            "OutboxPublishersEndpoint.kt",
            """
            import org.springframework.boot.actuate.endpoint.Access
            import org.springframework.boot.actuate.endpoint.annotation.ReadOperation
            import org.springframework.boot.actuate.endpoint.annotation.Selector
            import org.springframework.boot.actuate.endpoint.web.annotation.WebEndpoint

            @WebEndpoint(id = "outboxpublishers", defaultAccess = Access.NONE)
            internal class OutboxPublishersEndpoint {
                @ReadOperation
                fun status(@Selector registration: String) = registration
            }
            """.trimIndent()
        )
    }

    private fun unresolvedKeys(): List<String> = myFixture.doHighlighting().asSequence()
        .mapNotNull { it.description }
        .filter { it.contains("Cannot resolve key property") }
        .toList()

    private fun describe(element: PsiElement) = "${element::class.java.simpleName}[${element.text?.take(40)}]"
}
