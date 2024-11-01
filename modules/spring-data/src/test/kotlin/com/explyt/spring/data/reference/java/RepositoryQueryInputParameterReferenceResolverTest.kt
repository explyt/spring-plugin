package com.explyt.spring.data.reference.java

import com.explyt.jpa.ql.psi.JpqlInputParameterExpression
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiParameter
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.InjectionTestFixture

@TestDataPath(RepositoryQueryInputParameterReferenceResolverTest.TEST_DATA_PATH)
class RepositoryQueryInputParameterReferenceResolverTest : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springDataJpa_3_1_0,
    )

    fun testPositiveCases() {
        val vf = myFixture.copyFileToProject(
            "queryInputParameter/UserRepository_resolvable.java",
            "com/example/UserRepository.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        // named parameter to annotation
        moveToLanguageInjectionHost(":name")

        val nameParameter = injectionTestFixture.findInjectedInputParameter(":name")

        assertEquals(
            myFixture.findElementByText(
                "String firstName",
                PsiParameter::class.java
            ),
            nameParameter.reference.resolve()
        )

        // named parameter to field
        moveToLanguageInjectionHost(":lastName")

        val lastNameParameter = injectionTestFixture.findInjectedInputParameter(":lastName")

        assertEquals(
            myFixture.findElementByText(
                "String lastName",
                PsiParameter::class.java
            ),
            lastNameParameter.reference.resolve()
        )

        // numeric parameter
        moveToLanguageInjectionHost("u.age >= ?1")

        val ageParameter = injectionTestFixture.findInjectedInputParameter("?1")

        assertEquals(
            myFixture.findElementByText(
                "int age",
                PsiParameter::class.java
            ),
            ageParameter.reference.resolve()
        )
    }

    fun testNegativeCases() {
        val vf = myFixture.copyFileToProject(
            "queryInputParameter/UserRepository_unresolvable.java",
            "com/example/UserRepository.java"
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
        const val TEST_DATA_PATH = "testdata/java/reference"
    }
}

