/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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