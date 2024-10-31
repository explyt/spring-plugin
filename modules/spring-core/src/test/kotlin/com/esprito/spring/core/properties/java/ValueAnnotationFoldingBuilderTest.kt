package com.esprito.spring.core.properties.java

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.test.ExplytJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

class ValueAnnotationFoldingBuilderTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testCronPlaceholderIsFolded() {
        myFixture.addFileToProject("application.properties", "test.property=valueTestFolding")

        myFixture.configureByText(
            "TestBean.java", """            
            
            @${SpringCoreClasses.COMPONENT}
            public class TestBean {
              @${SpringCoreClasses.VALUE}("${'$'}{test.property}") String s;                             
            }
            """.trimIndent()
        )

        myFixture.checkHighlighting()

        val foldRegion = myFixture.editor.foldingModel.allFoldRegions
            .find { it.placeholderText == "valueTestFolding" }
        TestCase.assertNotNull(foldRegion)
    }
}