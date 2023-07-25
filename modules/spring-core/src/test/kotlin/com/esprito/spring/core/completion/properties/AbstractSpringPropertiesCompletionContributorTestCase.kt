package com.esprito.spring.core.completion.properties

import com.esprito.spring.test.EspritoJavaLightTestCase
import com.intellij.codeInsight.completion.CompletionType

abstract class AbstractSpringPropertiesCompletionContributorTestCase : EspritoJavaLightTestCase() {

    override fun getTestDataPath(): String = "testdata/completion/properties"

    protected fun doTest(vararg expectedLookupElements: String) {
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertEquals(
            expectedLookupElements.toSet(), lookupElementStrings!!.toSet()
        )
    }
}