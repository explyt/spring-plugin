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

package com.explyt.jpa.ql.reference.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.test.TestMetadata
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UField
import org.jetbrains.uast.toUElement
import java.io.File

private const val TEST_DATA_PATH = "reference/external"

/**
 * Tests references from jpql to jpa entities
 */
@TestMetadata(TEST_DATA_PATH)
abstract class JpqlExternalReferenceNavigationTest : ExplytKotlinLightTestCase() {
    class Jakarta : JpqlExternalReferenceNavigationTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpqlExternalReferenceNavigationTest() {
        override val libraries = arrayOf(
            TestLibrary.javax_persistence_2_2
        )
    }

    override fun getTestDataPath() = super.getTestDataPath() + TEST_DATA_PATH

    fun testEntityFrom() = doTest()
    fun testFieldWhere() = doTest()
    fun testSubField() = doTest()
    fun testSuperClassField() = doTest()
    fun testFieldGroupBy() = doTest()
    fun testFieldJoin() = doTest()
    fun testEntityInsert() = doTest()
    fun testFieldInsert() = doTest()
    fun testNoAlias() = doTest()

    @Suppress("UnstableApiUsage")
    private fun doTest() {
        myFixture.copyDirectoryToProject(
            "../model",
            "com/example"
        )

        val name = getTestName(true)
        val vf = myFixture.copyFileToProject(
            "$name.jpql",
            "$name.jpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val reference = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
            ?.reference
        assertNotNull(reference)
        reference!!

        val resolvedTo = reference.resolve()?.toUElement()
        assertNotNull(resolvedTo)
        resolvedTo!!


        val resultFilePath = "$testDataPath/${name}_navigation.txt"

        val expectedFqn = File(resultFilePath).readText()

        when (resolvedTo) {
            is UClass -> assertEquals(
                expectedFqn,
                resolvedTo.qualifiedName
            )

            is UField -> assertEquals(
                expectedFqn,
                resolvedTo.containingClass!!.qualifiedName + '.' + resolvedTo.name
            )

            else -> fail("Unknown element type: ${resolvedTo.sourcePsi.elementType}")
        }
    }
}