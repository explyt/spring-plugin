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

package com.explyt.spring.data.reference.kotlin

import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.InjectionTestFixture
import org.jetbrains.kotlin.psi.KtParameter

@TestDataPath(RepositoryQueryInputParameterReferenceResolverTest.TEST_DATA_PATH)
class RepositoryQueryInputParameterReferenceResolverTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springDataJpa_3_1_0,
    )

    fun testPositiveCases() {
        val vf = myFixture.copyFileToProject(
            "queryInputParameter/UserRepository_resolvable.kt",
            "com/example/UserRepository.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        // named parameter to annotation
        moveToLanguageInjectionHost(":name")

        val nameParameter = injectionTestFixture.findInjectedInputParameter(":name")

        assertEquals(
            myFixture.findElementByText(
                "firstName: String",
                KtParameter::class.java
            ),
            nameParameter.reference.resolve()
        )

        // named parameter to field
        moveToLanguageInjectionHost(":lastName")

        val lastNameParameter = injectionTestFixture.findInjectedInputParameter(":lastName")

        assertEquals(
            myFixture.findElementByText(
                "lastName: String",
                KtParameter::class.java
            ),
            lastNameParameter.reference.resolve()
        )

        // numeric parameter
        moveToLanguageInjectionHost("u.age >= ?1")

        val ageParameter = injectionTestFixture.findInjectedInputParameter("?1")

        assertEquals(
            myFixture.findElementByText(
                "age: Integer",
                KtParameter::class.java
            ),
            ageParameter.reference.resolve()
        )
    }

    fun testNegativeCases() {
        val vf = myFixture.copyFileToProject(
            "queryInputParameter/UserRepository_unresolvable.kt",
            "com/example/UserRepository.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        // named parameter 1
        moveToLanguageInjectionHost(":name")

        val nameParameter = injectionTestFixture.findInjectedInputParameter(":name")

        assertNull(
            nameParameter.reference.resolve()
        )

        // named parameter 2
        moveToLanguageInjectionHost(":lastName")

        val lastNameParameter = injectionTestFixture.findInjectedInputParameter(":lastName")

        assertNull(
            lastNameParameter.reference.resolve()
        )

        // numeric parameter
        moveToLanguageInjectionHost("u.age >= ?2")

        val ageParameter = injectionTestFixture.findInjectedInputParameter("?2")

        assertNull(
            ageParameter.reference.resolve()
        )
    }

    private fun InjectionTestFixture.findInjectedInputParameter(
        parameterText: String
    ): JpqlInputParameterExpression = openInFragmentEditor()
        .findElementByText(parameterText, JpqlInputParameterExpression::class.java)

    private fun moveToLanguageInjectionHost(hostContents: String) {
        editor.caretModel.moveToOffset(
            myFixture.findElementByText(
                hostContents,
                PsiLanguageInjectionHost::class.java
            )!!.textOffset + 1
        )
    }

    companion object {
        const val TEST_DATA_PATH = "testdata/kotlin/reference"
    }
}

