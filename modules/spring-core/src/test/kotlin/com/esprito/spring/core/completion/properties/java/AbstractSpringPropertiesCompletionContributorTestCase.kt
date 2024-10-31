package com.esprito.spring.core.completion.properties.java

import com.esprito.spring.test.ExplytJavaLightTestCase
import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.test.TestMetadata

abstract class AbstractSpringPropertiesCompletionContributorTestCase : ExplytJavaLightTestCase() {

    override fun getTestDataPath(): String = super.getTestDataPath() + "completion/properties"

    @TestMetadata("completion/properties")
    protected fun doTest(vararg expectedLookupElements: String) {
        myFixture.complete(CompletionType.BASIC)
        val lookupElementStrings = myFixture.lookupElementStrings
        assertNotNull(lookupElementStrings)
        assertEquals(
            expectedLookupElements.toSet(), lookupElementStrings!!.toSet()
        )
    }
}