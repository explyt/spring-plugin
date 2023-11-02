package com.esprito.spring.core.langinjection

import com.esprito.spring.core.language.profiles.ProfilesLanguage
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.testFramework.TestDataPath
import com.intellij.testFramework.fixtures.InjectionTestFixture

@TestDataPath(ProfilesAnnotationLanguageInjectorTest.TEST_DATA_PATH)
class ProfilesAnnotationLanguageInjectorTest : EspritoJavaLightTestCase() {
    override fun getTestDataPath(): String = TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testAnnotatedWithProfile() {
        val vf = myFixture.copyFileToProject(
            "AnnotatedWithProfile.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(ProfilesLanguage.INSTANCE.id)
    }

    fun testAnnotatedWithBean() {
        val vf = myFixture.copyFileToProject(
            "AnnotatedWithBean.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val injectionTestFixture = InjectionTestFixture(myFixture)

        injectionTestFixture.assertInjectedLangAtCaret(null)
    }

    companion object {
        const val TEST_DATA_PATH = "testdata/langinjection"
    }
}