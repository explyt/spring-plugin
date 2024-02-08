package com.esprito.spring.core.reference.kotlin

import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.psi.PsiPackage
import junit.framework.TestCase

class PackageReferenceContributorTest : EspritoKotlinLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "reference/package"

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testComponentScanProjectOneResolve() {
        val vf =
            myFixture.copyFileToProject("ComponentScanProject_File1.kt", "pack1/pack2/pack3/TestConfiguration.kt")
        myFixture.configureFromExistingVirtualFile(vf)

        val ref = file.findReferenceAt(myFixture.caretOffset)
        assertNotNull(ref)
        TestCase.assertEquals(true, ref?.isReferenceTo(myFixture.findPackage("pack1.pack2.pack3")))
    }

    fun testComponentScanProjectOneVariant() {
        val vf =
            myFixture.copyFileToProject("ComponentScanProject_File2.kt", "pack1/pack2/pack3_2/TestConfiguration.kt")
        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings

        TestCase.assertEquals(listOf("pack3_2"), lookupElementStrings)
    }

    fun testComponentScanProjectMultiVariant() {
        myFixture.copyFileToProject("ComponentScanProject_File1.kt", "pack1/pack2/pack3/TestConfiguration.kt")
        myFixture.copyFileToProject("ComponentScanProject_File2.kt", "pack1/pack2/pack3_2/TestConfiguration.kt")
        val vf =
            myFixture.copyFileToProject("ComponentScanProject_File3.kt", "pack1/pack2_2/pack3/TestConfiguration.kt")

        myFixture.configureFromExistingVirtualFile(vf)

        myFixture.complete(CompletionType.BASIC)

        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        TestCase.assertEquals(listOf("pack3", "pack3_2", "pack3"), lookupElementStrings)

        val lookupElements = myFixture.lookupElements
        assertNotNull(lookupElements)
        TestCase.assertEquals(
            listOf(
                "pack1.pack2.pack3",
                "pack1.pack2.pack3_2",
                "pack1.pack2_2.pack3"
            ),
            lookupElements?.map { (it.psiElement as PsiPackage).qualifiedName }
        )
    }

    fun testComponentScanProjectMultiResolve() {
        myFixture.copyFileToProject("ComponentScanProject_File1.kt", "pack1/pack2/pack3/TestConfiguration.kt")
        myFixture.copyFileToProject("ComponentScanProject_File2.kt", "pack1/pack2/pack3_2/TestConfiguration.kt")
        val vf =
            myFixture.copyFileToProject(
                "ComponentScanProject_File4.kt",
                "pack1/pack2_2/pack3_2/TestConfiguration.kt"
            )

        myFixture.configureFromExistingVirtualFile(vf)

        val ref = myFixture.getReferenceAtCaretPosition()

        assertNotNull(ref)
        assertNotNull(ref?.variants)
        TestCase.assertEquals(
            listOf(
                "pack1.pack2.pack3_2",
                "pack1.pack2_2.pack3_2"
            ),
            ref?.variants?.filterIsInstance<LookupElement>()?.map { (it.psiElement as PsiPackage).qualifiedName }
        )
    }
}