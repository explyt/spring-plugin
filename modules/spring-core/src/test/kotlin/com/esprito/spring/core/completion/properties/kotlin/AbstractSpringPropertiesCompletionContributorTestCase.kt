package com.esprito.spring.core.completion.properties.kotlin

import com.esprito.spring.test.ExplytKotlinLightTestCase
import com.intellij.codeInsight.completion.CompletionType
import org.jetbrains.kotlin.test.TestMetadata

abstract class AbstractSpringPropertiesCompletionContributorTestCase : ExplytKotlinLightTestCase() {

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