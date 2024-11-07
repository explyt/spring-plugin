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

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.psi.PsiPackage
import junit.framework.TestCase

class PackageReferenceContributorTest : ExplytKotlinLightTestCase() {

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

        val variants = myFixture.getReferenceAtCaretPosition()
            ?.variants
            ?.mapNotNull { it as? PsiPackage }
            ?.mapTo(mutableSetOf()) { it.qualifiedName }
        TestCase.assertNotNull(variants)

        TestCase.assertEquals(
            setOf(
                "pack1.pack2.pack3",
                "pack1.pack2.pack3_2",
                "pack1.pack2_2.pack3"
            ),
            variants
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
            setOf(
                "pack1.pack2.pack3_2",
                "pack1.pack2.pack3",
                "pack1.pack2_2.pack3_2"
            ),
            ref?.variants?.mapNotNull { it as? PsiPackage }?.mapTo(mutableSetOf()) { it.qualifiedName }
        )
    }
}