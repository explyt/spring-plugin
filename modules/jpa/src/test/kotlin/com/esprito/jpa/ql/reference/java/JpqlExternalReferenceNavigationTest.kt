package com.esprito.jpa.ql.reference.java

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiField
import com.intellij.psi.util.elementType
import org.jetbrains.kotlin.test.TestMetadata
import java.io.File

private const val TEST_DATA_PATH = "reference/external"

/**
 * Tests references from jpql to jpa entities
 */
@TestMetadata(TEST_DATA_PATH)
abstract class JpqlExternalReferenceNavigationTest : EspritoJavaLightTestCase() {
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
        var vf = myFixture.copyFileToProject(
            "$name.jpql",
            "$name.jpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val reference = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
            ?.reference
        assertNotNull(reference)
        reference!!

        val resolvedTo = reference.resolve()
        assertNotNull(resolvedTo)
        resolvedTo!!


        val resultFilePath = "$testDataPath/${name}_navigation.txt"

        val expectedFqn = File(resultFilePath).readText()

        when (resolvedTo) {
            is PsiClass -> assertEquals(
                expectedFqn,
                resolvedTo.qualifiedName
            )

            is PsiField -> assertEquals(
                expectedFqn,
                resolvedTo.containingClass!!.qualifiedName + '.' + resolvedTo.name
            )

            else -> fail("Unknown element type: ${resolvedTo.elementType}")
        }
    }
}