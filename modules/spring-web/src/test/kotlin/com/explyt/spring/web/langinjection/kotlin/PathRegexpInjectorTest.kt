/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.langinjection.kotlin

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.testFramework.fixtures.InjectionTestFixture
import org.intellij.lang.annotations.Language

class PathRegexpInjectorTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springWeb_6_0_7,
    )

    fun testRegexpInjectedInSingleStringPath() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Controller
            import org.springframework.web.bind.annotation.RequestMapping

            @Controller
            class TestController {

                @RequestMapping("/products/{id:[0-9]+}")
                fun getProduct(): String = "product"
            }
            """

        myFixture.configureByText("TestController.kt", code.trimIndent())

        InjectionTestFixture(myFixture).assertInjectedContent("[0-9]+")
    }

    fun testRegexpInjectedInConcatenatedStringPath() {
        @Language("kotlin") val code = """
            import org.springframework.stereotype.Controller
            import org.springframework.web.bind.annotation.RequestMapping

            @Controller
            class TestController {

                @RequestMapping("/products" + "/{id:[0-9]+}")
                fun getProduct(): String = "product"
            }
            """

        myFixture.configureByText("TestController.kt", code.trimIndent())

        InjectionTestFixture(myFixture).assertInjectedContent("[0-9]+")
    }
}
