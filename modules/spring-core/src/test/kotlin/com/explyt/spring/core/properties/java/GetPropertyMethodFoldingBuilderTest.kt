/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.properties.java

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import junit.framework.TestCase

class GetPropertyMethodFoldingBuilderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testProfileLessPropertiesWinsOverInactiveProfile() {
        myFixture.addFileToProject("application.properties", "database=h2")
        myFixture.addFileToProject("application-mysql.properties", "database=mysql")

        configureBean()

        TestCase.assertEquals("h2", foldedPlaceholderText())
    }

    fun testValueOnlyDefinedInProfileShowsProfileOrigin() {
        myFixture.addFileToProject("application-mysql.properties", "database=mysql")

        configureBean()

        TestCase.assertEquals("mysql (profile: mysql)", foldedPlaceholderText())
    }

    private fun configureBean() {
        myFixture.configureByText(
            "TestBean.java", """
            import org.springframework.core.env.Environment;

            public class TestBean {
              private Environment environment;

              String database() { return environment.getProperty("database"); }
            }
            """.trimIndent()
        )
        myFixture.doHighlighting()
    }

    private fun foldedPlaceholderText(): String {
        val document = myFixture.editor.document
        val regions = myFixture.editor.foldingModel.allFoldRegions
        return regions.firstOrNull { document.getText(it.textRange) == "\"database\"" }
            ?.placeholderText
            ?: throw AssertionError(
                "no folded key literal among " + regions.joinToString {
                    "[" + it.placeholderText + "] <- [" + document.getText(it.textRange) + "]"
                }
            )
    }
}
