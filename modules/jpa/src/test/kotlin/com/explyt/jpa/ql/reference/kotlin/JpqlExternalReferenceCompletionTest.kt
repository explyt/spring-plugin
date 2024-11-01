package com.explyt.jpa.ql.reference.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.codeInsight.lookup.LookupElement
import org.jetbrains.kotlin.test.TestMetadata
import org.junit.Assert
import org.junit.Ignore
import java.io.File
import java.util.*

private const val TEST_DATA_PATH = "reference/external"

/**
 * Tests references completion
 */
@TestMetadata(TEST_DATA_PATH)
@Ignore
abstract class JpqlExternalReferenceCompletionTest : ExplytKotlinLightTestCase() {
    class Jakarta : JpqlExternalReferenceCompletionTest() {
        override val libraries = arrayOf(
            TestLibrary.jakarta_persistence_3_1_0
        )
    }

    class Javax : JpqlExternalReferenceCompletionTest() {
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
    fun testConstructorClass() = doTest()
    fun testConstructorPackage() = doTest()

    @Suppress("UnstableApiUsage")
    private fun doTest() {
        myFixture.copyDirectoryToProject(
            "../model",
            "com/example"
        )

        val name = getTestName(true)
        var vf = myFixture.copyFileToProject(
            "$name.jpql",
            "$name.jpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)


        val reference = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
            ?.reference
        assertNotNull(reference)
        reference!!

        val actualVariants = reference.variants.mapTo(TreeSet()) {
            if (it is LookupElement) {
                it.lookupString
            } else {
                it.toString()
            }
        }.toArray()

        val resultFilePath = "$testDataPath/${name}_variants.txt"
        val expectedVariants = TreeSet(
            File(resultFilePath).readLines()
        ).toArray()

        Assert.assertArrayEquals(expectedVariants, actualVariants)
    }
}