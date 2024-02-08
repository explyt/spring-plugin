package com.esprito.jpa.ql.reference

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "reference/internal/rename"

/**
 * Tests references from jpql to itself
 */
@TestDataPath("\$CONTENT_ROOT/../../testdata/$TEST_DATA_PATH")
class JpqlInternalReferenceRenameTest : EspritoJavaLightTestCase() {
    override fun getTestDataPath() = "testdata/$TEST_DATA_PATH"

    fun testAliasInSelect() = doTest()
    fun testAliasInWhere() = doTest()
    fun testAliasInJoin() = doTest()
    fun testAliasToJoinFromWhere() = doTest()
    fun testAliasToJoinFromSelect() = doTest()
    fun testAliasInSubqueryWithShadowing() = doTest()

    @Suppress("UnstableApiUsage")
    private fun doTest() {
        val name = getTestName(true)
        val vf = myFixture.copyFileToProject(
            "$name.jpql",
            "$name.jpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val element = myFixture.elementAtCaret

        myFixture.renameElement(element, "newName")

        myFixture.checkResultByFile("$name.txt")
    }
}