/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
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