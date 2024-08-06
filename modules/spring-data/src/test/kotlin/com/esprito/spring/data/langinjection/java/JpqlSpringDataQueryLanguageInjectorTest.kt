package com.esprito.spring.data.langinjection.java

import com.esprito.jpa.ql.JpqlLanguage
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.InjectionTestFixture

@TestDataPath(JpqlSpringDataQueryLanguageInjectorTest.TEST_DATA_PATH)
class JpqlSpringDataQueryLanguageInjectorTest : EspritoJavaLightTestCase() {
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

        injectionTestFixture.assertInjectedLangAtCaret(null)
    }

    companion object {
        const val TEST_DATA_PATH = "testdata/java/langinjection"
    }
}