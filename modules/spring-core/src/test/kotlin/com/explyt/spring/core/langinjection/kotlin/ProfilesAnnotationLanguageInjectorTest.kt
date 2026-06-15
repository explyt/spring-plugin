/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.langinjection.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture
import org.jetbrains.kotlin.test.TestMetadata

private const val TEST_DATA_PATH = "langinjection"

@TestMetadata(TEST_DATA_PATH)
class ProfilesAnnotationLanguageInjectorTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testAnnotatedWithProfile() {
        val vf = myFixture.copyFileToProject(
            "AnnotatedWithProfile.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        //Profile language - not work rename(remove profile language injection)
        injectionTestFixture.assertInjectedLangAtCaret(null)
    }

    fun testAnnotatedWithBean() {
        val vf = myFixture.copyFileToProject(
            "AnnotatedWithBean.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(null)
    }

}