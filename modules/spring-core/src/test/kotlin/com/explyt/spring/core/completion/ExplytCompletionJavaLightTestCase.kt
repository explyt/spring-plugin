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