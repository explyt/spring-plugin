/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.reference.java

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

class YamlReferenceTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(
            TestLibrary.springBoot_3_1_1,
            TestLibrary.springContext_6_0_7,
            TestLibrary.resilience4j_2_2_0,
        )

    fun testRefInnerClass() {
        myFixture.copyFileToProject("LssConfigurationProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-<caret>configuration:                
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(nameClass, "setLssPlanConfiguration")
    }

    fun testRefInnerClassField() {
        myFixture.copyFileToProject("LssConfigurationProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-configuration:
    exa<caret>ct:
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setExact")
    }

    // in the next commit
    fun _testRefInnerClassFieldBoolean() {
        myFixture.copyFileToProject("LssConfigurationProperties.java")
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-configuration:
    is-exa<caret>ct:
            """.trimIndent()
        )
        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val name = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setExact")
    }

    fun testRefMap() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    inst<caret>ances:
      lssRateLimiterCreateTask:
        limit-for-period:
                    """.trimIndent()
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "instances")
    }

    fun testRefMapKey() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      lssRate<caret>LimiterCreateTask:
        limit-for-period:
                    """.trimIndent()
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "InstanceProperties")
    }

    fun testRefMapValue() {
        myFixture.configureByText(
            "application.yaml",
            """
resilience4j:
  ratelimiter:
    instances:
      lssRateLimiterCreateTask:
        limit-<caret>for-period:
                    """.trimIndent()
        )

        val ref = (file.findReferenceAt(myFixture.caretOffset) as? PsiMultiReference)
            ?.references?.asSequence()
            ?.mapNotNull {
                it as? ConfigurationPropertyKeyReference
            }?.firstOrNull()

        assertNotNull(ref)
        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val name = (multiResolve[0].element as? ConfigKeyPsiElement)?.name
        assertEquals(name, "setLimitForPeriod")
    }
}