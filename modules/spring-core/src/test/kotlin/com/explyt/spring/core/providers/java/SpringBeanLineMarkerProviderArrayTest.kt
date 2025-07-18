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

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString

class SpringBeanLineMarkerProviderArrayTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerOptional_toAutowired_A_arrayA() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    A[] arrayA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayA" }
        }.size, 2)
    }

    fun testLineMarkerOptional_toBean_A_arrayA() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    A[] arrayA;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayE() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    E[] arrayE;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayE" }
        }.size, 6)
    }

    fun testLineMarkerOptional_toBean_arrayE() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    E[] arrayE;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 4)
    }

    fun testLineMarkerOptional_toAutowired_arrayB() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    B[] arrayB;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayB" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_arrayB() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    B[] arrayB;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayD() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    D[] arrayD;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayD" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_arrayD() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    D[] arrayD;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    fun testLineMarkerOptional_toAutowired_arrayI() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    I[] arrayI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayI" }
        }.size, 4)
    }

    fun testLineMarkerOptional_toBean_arrayI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    I[] arrayI;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_beanArrayC() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    C[] arrayC;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayC" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_beanArrayC() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    C[] arrayC;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_list_beanArrayC() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<C[]> arrayC;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayC" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_list_beanArrayC() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<C[]> arrayC;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayWithBeanI() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    WithBeanI[] arrayWithBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayWithBeanI" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_arrayWithBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    WithBeanI[] arrayWithBeanI;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayWithoutBeanI() {
        myFixture.configureByText(
            "FooArray.java",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    WithoutBeanI[] arrayWithoutBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.java", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayWithoutBeanI" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_arrayWithoutBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    WithoutBeanI[] arrayWithoutBeanI;
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.java", getArrayClasses())
        myFixture.configureByText("TestArray.java", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    private fun getArrayClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean

            interface I {}

            @Component
            class A extends E implements I {}

            @Component
            class E {}

            class B extends E implements I {}

            class C extends E implements I {}

            class D extends E implements I {}

            interface WithBeanI {}

            interface WithoutBeanI {}

            @Configuration
            class TestConfiguration {
                @Bean
                B b() { return new B(); }
                
                @Bean
                E d() { return new D(); }
                
                @Bean
                A[] arrayA() { return new A[1]; }

                @Bean
                C[] arrayC() { return new C[2]; }
                
                @Bean 
                WithBeanI[] beanArrayWithBeanI() { return new WithBeanI[50]; }
            }
        """.trimIndent()
    }

}