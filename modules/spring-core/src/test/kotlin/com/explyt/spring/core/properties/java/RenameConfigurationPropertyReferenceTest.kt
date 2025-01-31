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

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

class RenameConfigurationPropertyReferenceTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "properties/rename"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testRenameFromYaml() {
        myFixture.configureByFile(
            "LssConfigurationProperties.java",
        )
        myFixture.addFileToProject(
            "application-config.properties",
            "lss.lss-plan-configuration.exact=false"
        )
        myFixture.configureByText(
            "application.yaml",
            """
lss:
  lss-plan-configuration:
    exa<caret>ct: true
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
        myFixture.renameElement(resolveResult.element!!, "setExactNew")

        myFixture.checkResult(
            """
lss:
  lss-plan-configuration:
    exact-new: true
""".trimIndent()
        )

        myFixture.checkResultByFile("LssConfigurationProperties.java", "LssConfigurationProperties_after.java", true)
        myFixture.checkResultByFile("application-config.properties", "application-config_after.properties", true)
    }

    fun testRenameFromProperties() {
        myFixture.configureByFile(
            "LssConfigurationProperties.java",
        )
        myFixture.addFileToProject(
            "application-config.yaml",
            """
lss:
  lss-plan-configuration:
    exact: true
""".trimIndent()
        )
        myFixture.configureByText(
            "application-config.properties",
            "lss.lss-plan-configuration.exa<caret>ct=false"
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
        myFixture.renameElement(resolveResult.element!!, "setExactNew")

        myFixture.checkResult(
            """
lss.lss-plan-configuration.exact-new=false
""".trimIndent()
        )

        myFixture.checkResultByFile("LssConfigurationProperties.java", "LssConfigurationProperties_after.java", true)
        myFixture.checkResultByFile("application-config.yaml", "application-config_after.yaml", true)
    }

    fun testRenameFromMethod() {
        myFixture.addFileToProject(
            "application-config.properties",
            "lss.lss-plan-configuration.exact=false"
        )
        myFixture.addFileToProject(
            "application-config.yaml",
            """
lss:
  lss-plan-configuration:
    exact: true
""".trimIndent()
        )
        myFixture.configureByFile(
            "LssConfigurationProperties.java",
        )
        val element = myFixture.findElementByText("setExact", PsiElement::class.java)

        assertNotNull(element)
        myFixture.renameElement(element!!, "setExactNew")

        myFixture.checkResultByFile("LssConfigurationProperties.java", "LssConfigurationProperties_after.java", true)
        myFixture.checkResultByFile("application-config.yaml", "application-config_after.yaml", true)
        myFixture.checkResultByFile("application-config.properties", "application-config_after.properties", true)
    }

}