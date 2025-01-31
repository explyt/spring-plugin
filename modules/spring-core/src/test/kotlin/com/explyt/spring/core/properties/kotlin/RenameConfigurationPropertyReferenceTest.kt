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

package com.explyt.spring.core.properties.kotlin

import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiElement
import com.intellij.psi.impl.source.resolve.reference.impl.PsiMultiReference

class RenameConfigurationPropertyReferenceTest : ExplytKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "properties/rename"

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testRenameFromYaml() {
        myFixture.configureByFile(
            "LssConfigurationProperties.kt",
        )
        myFixture.addFileToProject(
            "application-config.properties",
            """
lss.mode-for-studio=EDGE7
lss.token-for-studio=EDGE10
            """.trimIndent()
        )
        myFixture.configureByText(
            "application-config.yaml",
            """
lss:
  mode-for-stu<caret>dio: EDGE7
  token-for-studio: EDGE10
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
        myFixture.renameElement(resolveResult.element!!, "setModeForStudioNew")

        myFixture.checkResultByFile("LssConfigurationProperties.kt", "LssConfigurationProperties_after.kt", true)
        myFixture.checkResultByFile("application-config.yaml", "application-config_after.yaml", true)
        myFixture.checkResultByFile("application-config.properties", "application-config_after.properties", true)
    }

    fun testRenameFromProperties() {
        myFixture.configureByFile(
            "LssConfigurationProperties.kt",
        )
        myFixture.addFileToProject(
            "application-config.yaml",
            """
lss:
  mode-for-studio: EDGE7
  token-for-studio: EDGE10
""".trimIndent()
        )
        myFixture.configureByText(
            "application-config.properties",
            """
lss.mode-for-stu<caret>dio=EDGE7
lss.token-for-studio=EDGE10
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
        myFixture.renameElement(resolveResult.element!!, "setModeForStudioNew")

        myFixture.checkResultByFile("LssConfigurationProperties.kt", "LssConfigurationProperties_after.kt", true)
        myFixture.checkResultByFile("application-config.yaml", "application-config_after.yaml", true)
        myFixture.checkResultByFile("application-config.properties", "application-config_after.properties", true)
    }

    fun testRenameFromMethod() {
        myFixture.addFileToProject(
            "application-config.yaml",
            """
lss:
  mode-for-studio: EDGE7
  token-for-studio: EDGE10
""".trimIndent()
        )
        myFixture.addFileToProject(
            "application-config.properties",
            """
lss.mode-for-studio=EDGE7
lss.token-for-studio=EDGE10
            """.trimIndent()
        )
        myFixture.configureByFile(
            "LssConfigurationProperties.kt",
        )
        val element = myFixture.findElementByText("modeForStudio", PsiElement::class.java)

        assertNotNull(element)
        myFixture.renameElement(element!!, "modeForStudioNew")

        myFixture.checkResultByFile("LssConfigurationProperties.kt", "LssConfigurationProperties_after.kt", true)
        myFixture.checkResultByFile("application-config.yaml", "application-config_after.yaml", true)
        myFixture.checkResultByFile("application-config.properties", "application-config_after.properties", true)
    }

}