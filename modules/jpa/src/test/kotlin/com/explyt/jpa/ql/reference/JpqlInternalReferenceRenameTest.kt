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
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.util.Computable
import com.intellij.openapi.vfs.readText
import com.intellij.testFramework.TestDataPath

private const val TEST_DATA_PATH = "reference/internal/rename"

/**
 * Tests references from jpql to itself
 */
@TestDataPath("\$CONTENT_ROOT/../../testdata/$TEST_DATA_PATH")
class JpqlInternalReferenceRenameTest : ExplytJavaLightTestCase() {
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
            "$name.ejpql",
            "$name.ejpql"
        )
        myFixture.configureFromExistingVirtualFile(vf)

        val element = myFixture.elementAtCaret

        myFixture.renameElement(element, "newName")

        val vfResult = myFixture.copyFileToProject("$name.txt", "$name.txt")
        val resultText = ApplicationManager.getApplication().runReadAction(Computable { vfResult.readText() })
        myFixture.checkResult(resultText)
    }
}