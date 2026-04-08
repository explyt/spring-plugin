/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.completion

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.completion.CompletionType
import com.intellij.codeInsight.lookup.Lookup
import com.intellij.codeInsight.lookup.LookupManager
import com.intellij.codeInsight.lookup.impl.LookupImpl

abstract class ExplytCompletionJavaLightTestCase : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springBootAutoConfigure_3_1_1, TestLibrary.springContext_6_0_7)

    protected fun doTest(init: TestModel.() -> Unit) {
        val model = TestModel()
        model.init()

        myFixture.configureByText(model.fileName, model.initSource)

        val lookupElements = myFixture.complete(CompletionType.BASIC)
        assertNotNull(lookupElements)

        val lookupElementStrings = lookupElements.map { it.lookupString }

        assertEquals(
            model.expectedLookupElements, lookupElementStrings.toSet()
        )

        getActiveLookup().currentItem = lookupElements[0]
        getActiveLookup().finishLookup(Lookup.NORMAL_SELECT_CHAR)
        myFixture.checkResult(model.sourceAfterComplete)
    }

    private fun getActiveLookup(): LookupImpl {
        return LookupManager.getActiveLookup(myFixture.editor) as LookupImpl
    }

    protected class TestModel {
        lateinit var fileName: String
        lateinit var initSource: String
        lateinit var sourceAfterComplete: String
        lateinit var expectedLookupElements: Set<String>
    }

}