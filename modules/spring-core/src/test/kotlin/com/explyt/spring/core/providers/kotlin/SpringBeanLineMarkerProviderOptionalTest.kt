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
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString

class SpringBeanLineMarkerProviderOptionalTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerOptional_toAutowired_A_optA() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optA: java.util.Optional<A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                    lateinit var optA: java.util.Optional<A>
                }
            """.trimIndent()

        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
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
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun optC(cFooOptionalParameter: java.util.Optional<C>) {}
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                    fun optC(cFooOptionalParameter: java.util.Optional<C>) {}
                }
            """.trimIndent()

        myFixture.configureByText("FooOptional.kt", getOptionalClasses() + fooOptional)
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
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun optB(bFooOptionalParameter: java.util.Optional<B>) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                    fun optB(bFooOptionalParameter: java.util.Optional<B>) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
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
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun optD(dFooOptionalParameter: java.util.Optional<D>) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "dFooOptionalParameter" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_D_dParameter() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun optD(dFooOptionalParameter: java.util.Optional<D>) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    fun testLineMarkerOptional_toAutowired_A_aParameter() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun optA(aFooOptionalParameter: java.util.Optional<A>) {} 
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                    fun optA(aFooOptionalParameter: java.util.Optional<A>) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses() + fooOptional)
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
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalInter: java.util.Optional<WithoutBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalInter" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_WithoutBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalInter: java.util.Optional<WithoutBeanI>
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.java", getOptionalClasses())
        myFixture.configureByText("TestOptional.java", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isEmpty())
    }

    fun testLineMarkerOptional_toAutowired_optionalI() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalI: java.util.Optional<I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                    lateinit var optionalI: java.util.Optional<I>
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_optionalListI() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalListI: java.util.Optional<MutableList<I>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                    lateinit var optionalListI: java.util.Optional<MutableList<I>>
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_optionalOptionalListI() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalOptionalListI: java.util.Optional<java.util.Optional<MutableList<I>>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalOptionalListI" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_optionalOptionalListI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalOptionalListI: java.util.Optional<java.util.Optional<MutableList<I>>>
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    fun testLineMarkerOptional_toAutowired_optionalMapStringI() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalMapStringI: Optional<Map<String, I>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
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
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalMapStringI: Optional<Map<String, I>>
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_optionalOptionalMapStringI() {
        myFixture.configureByText(
            "FooOptional.kt",
            """
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalOptionalMapStringI: Optional<Optional<Map<String, I>>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestOptional.kt", getOptionalClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalOptionalMapStringI" }
        }.size, 0)
    }

    fun testLineMarkerOptional_toBean_optionalOptionalMapStringI() {
        val fooOptional = """
                import java.util.Optional;

                @org.springframework.stereotype.Component
                class FooOptional {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalOptionalMapStringI: Optional<Optional<Map<String, I>>>
                }
            """.trimIndent()
        myFixture.configureByText("FooOptional.kt", getOptionalClasses())
        myFixture.configureByText("TestOptional.kt", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    private fun getOptionalClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean

            interface I {}

            @Component
            open class E {}

            @Component
            class A: E(),  I

            class B: E(), I {}

            class C: E(), I {}

            class D: E(), I {}

            interface WithoutBeanI {}

            @Configuration
            open class TestConfigurationKt {
                @Bean
                open fun dBean(): E { return D() }

                @Bean
                open fun bBean():B { return B() }
            }
        """.trimIndent()
    }

}