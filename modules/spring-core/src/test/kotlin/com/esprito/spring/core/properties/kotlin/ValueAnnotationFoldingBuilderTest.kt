package com.esprito.spring.core.properties.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

class ValueAnnotationFoldingBuilderTest : EspritoKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testCronPlaceholderIsFolded() {
        myFixture.addFileToProject("application.properties", "test.property=valueTestFolding")

        val propertyString = "\\\${test.property}"
        myFixture.configureByText(
            "TestBean.kt", """            
            
            @${SpringCoreClasses.COMPONENT}
            class TestBean {
              @${SpringCoreClasses.VALUE}("$propertyString") lateinit var s: String                             
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()
        val foldRegion = myFixture.editor.foldingModel.allFoldRegions
            .find { it.placeholderText == "valueTestFolding" }
        TestCase.assertNotNull(foldRegion)
    }
}