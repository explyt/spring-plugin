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

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.web.inspections.SpringOmittedPathVariableParameterInspection
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringOmittedPathVariableParameterInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7, TestLibrary.springWeb_6_0_7
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringOmittedPathVariableParameterInspection::class.java)
    }

    @TestMetadata("pathVariableController")
    fun testPathVariableController() = doTest(SpringOmittedPathVariableParameterInspection())

    fun testKotlinStringExpression() {
        @Language("kotlin") val code = """
            import org.springframework.web.bind.annotation.PostMapping
            const val str = "str" 
            
            @${SpringCoreClasses.COMPONENT}            
            class SpringComponent {
                                 
                @PostMapping("post1/${'$'}{str}")
                fun postStr(): String {
                    return "postStr"
                }
            }
            """.trimIndent()
        myFixture.configureByText("SpringComponent.kt", code)
        myFixture.testHighlighting("SpringComponent.kt")
    }
}