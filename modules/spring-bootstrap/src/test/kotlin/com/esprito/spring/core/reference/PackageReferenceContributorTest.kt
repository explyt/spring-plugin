package com.esprito.spring.core.reference

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import junit.framework.TestCase

class PackageReferenceContributorTest : EspritoJavaLightTestCase() {

    override fun getTestDataPath(): String = "testdata/reference"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testComponentScanProjectVariants() {
        val vf = myFixture.copyFileToProject("ComponentScanProjectVariants.java", "com/example/TestConfiguration.java")
        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        TestCase.assertEquals(listOf("example"), lookupElementStrings)
    }

    fun testComponentScanProjectResolve() {
        val vf = myFixture.copyFileToProject("ComponentScanProjectResolve.java", "com/example/TestConfiguration.java")
        myFixture.configureFromExistingVirtualFile(vf)

        val ref = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull(ref)
        //TODO check reference to package
    }
}