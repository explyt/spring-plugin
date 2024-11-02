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

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class GetBeanLineMarkerProviderTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String =
        super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testGetByName() {
        myFixture.configureByText(
            "FooComponent.kt",
            """
                import org.springframework.context.ApplicationContext
                import org.springframework.stereotype.Component
                
                @Component
                class Foo

                @Component
                 class JustTest {
                    fun justTest(context: ApplicationContext) {
                        val foo = context.getBean("foo")
                        val foo1 = context.getBean("foo") as Foo
                        val foo2 = context.getBean(Foo::class.java)
                        val foo3: Any = context.getBean("foo", Foo::class.java)
                        val foo4 = context.getBean("foo", "")
                        val foo5 = context.getBean(Foo::class.java, "")
                    }
                }
            """.trimIndent()
        )
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        assertEquals(
            6,
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it == "Foo" }
            }.size
        )
    }

}