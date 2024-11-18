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
            "$name/from.jpql",
            "from.jpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val reference = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
            ?.reference
        assertNotNull(reference)
        reference!!

        val resolvedTo = reference.resolve() ?: reference.element

        vf = myFixture.copyFileToProject(
            "$name/to.jpql",
            "to.jpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val target = TargetElementUtil.findTargetElement(editor, TargetElementUtilBase.ELEMENT_NAME_ACCEPTED)
        assertNotNull(target)
        target!!

        assertEquals(target.text, resolvedTo.text)
        assertEquals(target.textOffset, resolvedTo.textOffset)
    }
}