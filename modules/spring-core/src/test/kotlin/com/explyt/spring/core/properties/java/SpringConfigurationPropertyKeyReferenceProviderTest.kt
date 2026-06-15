/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.properties.providers.ConfigKeyPsiElement
import com.explyt.spring.core.properties.providers.ConfigurationPropertyKeyReference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary

class SpringConfigurationPropertyKeyReferenceProviderTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "properties"

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7)

    fun testPropertyPrefixKeys() {
        myFixture.configureByText(
            "application.properties",
            """
            logging.level<caret>.root=debug
            """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ConfigurationPropertyKeyReference
        assertNotNull(ref)

        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(nameClass, "LoggingApplicationListener")
    }

    fun testYamlPrefixKeys() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level<caret>:
    root: debug
            """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ConfigurationPropertyKeyReference
        assertNotNull(ref)

        val multiResolve = ref!!.multiResolve(true)
        assertEquals(1, multiResolve.size)
        val resolveResult = multiResolve[0]
        val nameClass = (resolveResult.element as? ConfigKeyPsiElement)?.name
        assertEquals(nameClass, "LoggingApplicationListener")
    }

    fun testYamlPrefixKeysNotExist() {
        myFixture.configureByText(
            "application.yaml",
            """
logging:
  level:
    root: <caret> debug
            """.trimIndent()
        )

        val ref = file.findReferenceAt(myFixture.caretOffset) as? ConfigurationPropertyKeyReference
        assertNull(ref)
    }

}