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

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderOptionalTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerOptional_toAutowired_A_optA() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<A> optA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optA" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_A_optA() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<A> optA;
                }
            """.trimIndent()

        myFixture.configureByText("FooOptional.java", getOptionalClasses())
        myFixture.configureByText("TestOptional.java", fooOptional)
        myFixture.doHighlighting()


        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toAutowired_C_cParameter() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optC(java.util.Optional<C> cFooOptionalParameter) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "cFooOptionalParameter" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_C_cParameter() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optC(java.util.Optional<C> cFooOptionalParameter) {} 
                }
            """.trimIndent()

        myFixture.configureByText("FooOptional.java", getOptionalClasses() + fooOptional)
        myFixture.doHighlighting()


        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "C" }
        }.size, 0)

    }

    fun testLineMarkerOptional_toAutowired_B_bParameter() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optB(java.util.Optional<B> bFooOptionalParameter) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "bFooOptionalParameter" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_B_bParameter() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optB(java.util.Optional<B> bFooOptionalParameter) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.java", getOptionalClasses())
        myFixture.configureByText("TestOptional.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "bBean()" }
        }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "B" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toAutowired_D_dParameter() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optD(java.util.Optional<D> dFooOptionalParameter) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "dFooOptionalParameter" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toAutowired_A_aParameter() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optA(java.util.Optional<A> aFooOptionalParameter) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "aFooOptionalParameter" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toBean_A_aParameter() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optA(java.util.Optional<A> aFooOptionalParameter) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.java", getOptionalClasses() + fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerOptional_toAutowired_WithoutBeanI() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<WithoutBeanI> optionalInter; 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalInter" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toAutowired_optionalI() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<I> optionalI; 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalI" }
        }.size, 2)
    }

    fun testLineMarkerOptional_toBean_optionalI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<I> optionalI; 
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.java", getOptionalClasses())
        myFixture.configureByText("TestOptional.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_optionalListI() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<java.util.List<I>> optionalListI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalListI" }
        }.size, 2)
    }

    fun testLineMarkerOptional_toBean_optionalListI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<java.util.List<I>> optionalListI;
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.java", getOptionalClasses())
        myFixture.configureByText("TestOptional.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_optionalOptionalListI() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<java.util.Optional<java.util.List<I>>> optionalOptionalListI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalOptionalListI" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toAutowired_optionalMapStringI() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                import java.util.Map;
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    Optional<Map<String, I>> optionalMapStringI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalMapStringI" }
        }.size, 2)
    }

    fun testLineMarkerOptional_toBean_optionalMapStringI() {
        val fooOptional = """
                import java.util.Map;
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    Optional<Map<String, I>> optionalMapStringI;
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.java", getOptionalClasses())
        myFixture.configureByText("TestOptional.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_optionalOptionalMapStringI() {
        myFixture.configureByText(
            "FooOptional.java",
            """
                import java.util.Map;
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    Optional<Optional<Map<String, I>>> optionalOptionalMapStringI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.java", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalOptionalMapStringI" }
        }.size, 0)
    }

    private fun getOptionalClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean

            interface I {}

            @Component
            class E {}

            @Component
            class A extends E implements I {}

            class B extends E implements I {}

            class C extends E implements I {}

            class D extends E implements I {}

            @Configuration
            class TestConfiguration {
                @Bean E dBean() { return new D(); }
                @Bean B bBean() { return new B(); }
            }

            interface WithoutBeanI {}
        """.trimIndent()
    }

}