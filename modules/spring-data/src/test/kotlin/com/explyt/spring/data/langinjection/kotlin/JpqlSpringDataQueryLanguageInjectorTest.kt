package com.explyt.spring.data.langinjection.kotlin

import com.explyt.jpa.ql.JpqlLanguage
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
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
            "QueryRepository.kt",
            "QueryRepository.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(JpqlLanguage.INSTANCE.id)
    }

    fun testNoInjection() {
        val vf = myFixture.copyFileToProject(
            "NativeQueryRepository.kt",
            "NativeQueryRepository.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(null)
    }

    companion object {
        const val TEST_DATA_PATH = "testdata/kotlin/langinjection"
    }
}