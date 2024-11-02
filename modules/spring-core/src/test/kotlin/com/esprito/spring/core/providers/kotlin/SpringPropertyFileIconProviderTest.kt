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

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.providers.SpringPropertyFileIconProvider
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiFile
import org.jetbrains.yaml.psi.YAMLFile

class SpringPropertyFileIconProviderTest : ExplytKotlinLightTestCase() {

    private lateinit var iconProvider: SpringPropertyFileIconProvider

    override fun setUp() {
        super.setUp()
        iconProvider = SpringPropertyFileIconProvider()
    }

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testReturnIconInPropertyYaml() {
        val yamlFile = myFixture.configureByText("application-test.yaml", "").virtualFile
        val psiFile = myFixture.psiManager.findFile(yamlFile) as YAMLFile

        val icon = iconProvider.getIcon(psiFile, 0)

        assertNotNull(icon)
        assertEquals(icon, SpringIcons.SpringSetting)
    }

    fun testNotReturnIconInPropertyYaml() {
        val yamlFile = myFixture.configureByText("other-application.yaml", "").virtualFile
        val psiFile = myFixture.psiManager.findFile(yamlFile) as YAMLFile
        val icon = iconProvider.getIcon(psiFile, 0)

        assertNull(icon)
    }

    fun testReturnIconInPropertyProperty() {
        val file = myFixture.configureByText("application-test.properties", "").virtualFile
        val propertiesFile = myFixture.psiManager.findFile(file)

        val icon = iconProvider.getIcon(propertiesFile as PsiFile, 0)

        assertNotNull(icon)
        assertEquals(icon, SpringIcons.SpringSetting)
    }

    fun testNotReturnIconInPropertyProperty() {
        val file = myFixture.configureByText("other-application.properties", "").virtualFile
        val propertiesFile = myFixture.psiManager.findFile(file)
        val icon = iconProvider.getIcon(propertiesFile as PsiFile, 0)

        assertNull(icon)
    }
}