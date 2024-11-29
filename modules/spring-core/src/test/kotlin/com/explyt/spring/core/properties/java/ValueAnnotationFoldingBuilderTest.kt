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

package com.explyt.spring.core.properties.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
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