/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.jpa.ql.reference

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.intellij.codeInsight.TargetElementUtil
import com.intellij.codeInsight.TargetElementUtilBase
import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "reference/internal/navigation"

/**
 * Tests references from jpql to itself
 */
@TestDataPath("\$CONTENT_ROOT/../../testdata/$TEST_DATA_PATH")
class JpqlInternalReferenceNavigationTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath() = "testdata/$TEST_DATA_PATH"

    fun testAliasInSelect() = doTest()
    fun testAliasInWhere() = doTest()
    fun testAliasInJoin() = doTest()
    fun testAliasToJoinFromWhere() = doTest()
    fun testAliasToJoinFromSelect() = doTest()
    fun testAliasInSubqueryWithShadowing() = doTest()
    fun testSubqueryAliasUnavailableInParent() = doTest()

    @Suppress("UnstableApiUsage")
    private fun doTest() {
        val name = getTestName(true)
        var vf = myFixture.copyFileToProject(
            "$name/from.ejpql",
            "from.ejpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val reference = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
            ?.reference
        assertNotNull(reference)
        reference!!

        val resolvedTo = reference.resolve() ?: reference.element

        vf = myFixture.copyFileToProject(
            "$name/to.ejpql",
            "to.ejpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val target = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
        assertNotNull(target)
        target!!

        assertEquals(target.text, resolvedTo.text)
        assertEquals(target.textOffset, resolvedTo.textOffset)
    }
}