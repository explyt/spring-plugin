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

package com.explyt.spring.data.langinjection.java

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.sql.SqlExplytLanguage
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.InjectionTestFixture

@TestDataPath(JpqlSpringDataQueryLanguageInjectorTest.TEST_DATA_PATH)
class JpqlSpringDataQueryLanguageInjectorTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springDataJpa_3_1_0,
    )

    fun testQueryInjection() {
        val vf = myFixture.copyFileToProject(
            "QueryRepository.java",
            "QueryRepository.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testNoInjection() {
        val vf = myFixture.copyFileToProject(
            "NativeQueryRepository.java",
            "NativeQueryRepository.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(SqlExplytLanguage.INSTANCE.id)
    }

    companion object {
        const val TEST_DATA_PATH = "testdata/java/langinjection"
    }
}