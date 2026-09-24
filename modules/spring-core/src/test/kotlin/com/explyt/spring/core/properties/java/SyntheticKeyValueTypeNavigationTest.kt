/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiMember
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

/**
 * `management.endpoint.<id>.access` is synthesised by Spring's endpoint infrastructure: its `sourceType` names the
 * endpoint class, which declares no `access` member at all. The value type is the answer the key asks for.
 */
class SyntheticKeyValueTypeNavigationTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootActuatorAutoConfigure_4_1_0)

    fun testSyntheticEndpointAccessKeyNavigatesToValueType() {
        val target = resolveKey("management.endpoint.httpexchanges.acce<caret>ss")

        assertTrue("expected an enum class, got: $target", (target as? PsiClass)?.isEnum == true)
        assertEquals("Access", target?.name)
    }

    fun testYamlSyntheticEndpointAccessKeyNavigatesToValueType() {
        myFixture.configureByText(
            "application.yaml",
            """
management:
  endpoint:
    httpexchanges:
      acce<caret>ss: read-only
            """.trimIndent()
        )

        val target = resolveAtCaret()

        assertTrue("expected an enum class, got: $target", (target as? PsiClass)?.isEnum == true)
        assertEquals("Access", target?.name)
    }

    fun testDeclaredKeyStillNavigatesToItsMember() {
        val target = resolveKey("management.endpoints.jmx.doma<caret>in")

        assertFalse("expected a member, not the source class, got: $target", target is PsiClass)
        assertEquals("JmxEndpointProperties", (target?.containingClass)?.name)
    }

    fun testDeclaredRolesKeyStillNavigatesToItsMember() {
        val target = resolveKey("management.endpoint.env.rol<caret>es")

        assertFalse("expected a member, not the source class, got: $target", target is PsiClass)
        assertEquals("EnvironmentEndpointProperties", (target?.containingClass)?.name)
    }

    private fun resolveKey(keyWithCaret: String): PsiMember? {
        myFixture.configureByText("application.properties", "$keyWithCaret=")

        return resolveAtCaret()
    }

    private fun resolveAtCaret(): PsiMember? {
        val reference = file.findReferenceAt(myFixture.caretOffset)
        val keyReference = requireNotNull(
            reference as? ConfigurationPropertyKeyReference
                ?: (reference as? PsiMultiReference)?.references
                    ?.filterIsInstance<ConfigurationPropertyKeyReference>()?.firstOrNull()
        ) { "expected a ConfigurationPropertyKeyReference on the key, got: $reference" }

        val resolved = keyReference.multiResolve(false).map { it.element }
        assertEquals("expected a single resolve result, got: $resolved", 1, resolved.size)
        val configKeyElement = requireNotNull(resolved.single() as? ConfigKeyPsiElement) {
            "expected a ConfigKeyPsiElement, got: ${resolved.single()}"
        }
        return configKeyElement.parent as? PsiMember
    }
}
