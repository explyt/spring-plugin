package com.esprito.jpa.ql.reference.kotlin

import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
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
abstract class JpqlExternalReferenceNavigationTest : EspritoKotlinLightTestCase() {
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