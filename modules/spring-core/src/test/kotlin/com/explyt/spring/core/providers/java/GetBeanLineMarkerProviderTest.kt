/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class GetBeanLineMarkerProviderTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testGetByName() {
        myFixture.configureByText(
            "FooComponent.java",
            """
                import org.springframework.context.ApplicationContext;
                import org.springframework.stereotype.Component;
                
                @Component
                public class Foo { }
                
                @Component
                class JustTest {
                    public void justTest(ApplicationContext context) {
                        Object foo = context.getBean("foo");
                        Foo foo1 = (Foo) context.getBean("foo");
                        Foo foo2 = context.getBean(Foo.class);
                        Object foo3 = context.getBean("foo", Foo.class);
                        Object foo4 = context.getBean("foo", "");
                        Foo foo5 = context.getBean(Foo.class, "");
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