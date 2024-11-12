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

package com.explyt.jpa.ql.langinjection.kotlin

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture

abstract class JpqNamedQueryLanguageInjectorTest : ExplytJavaLightTestCase() {
    class Jakarta : JpqNamedQueryLanguageInjectorTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpqNamedQueryLanguageInjectorTest() {
        override val libraries = arrayOf(
            TestLibrary.javax_persistence_2_2
        )
    }

    fun testInjection() {
        val vf = myFixture.configureByText(
            "Test.kt",
            """
import jakarta.persistence.*
import javax.persistence.*

@Entity
@NamedQuery(name = "Department.loadAll", query = "SELECT d FROM " + "<caret>Department d")
class Department
            """.trimIndent()
        )

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }
}