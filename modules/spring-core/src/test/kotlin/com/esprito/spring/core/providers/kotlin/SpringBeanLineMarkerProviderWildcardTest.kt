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
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString

class SpringBeanLineMarkerProviderWildcardTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerWildcard_toAutowired_setAllWildcard() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcard: Set<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcard" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcard() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcard: Set<*>
                }
            """.trimIndent()
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetI()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardExtendsA() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcardExtendsA: Set<out A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardExtendsA" }
        }.size, 1)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardExtendsA() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcardExtendsA: Set<out A>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardExtendsE() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcardExtendsE: Set<out E>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardExtendsE" }
        }.size, 4)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardExtendsE() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcardExtendsE: Set<out E>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 4)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardExtendsI() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcardExtendsI: Set<out I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardExtendsI" }
        }.size, 3)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardExtendsI() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllWildcardExtendsI: Set<out I>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcard() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcard: Map<String, *>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 7
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcard" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_mapWildcard() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcard: Map<String, *>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringWithBeanI()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_optionalMapWildcard() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                import java.util.Optional;
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalMapWildcard: java.util.Optional<Map<String, *>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 5
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalMapWildcard" }
        }.size, 0)
    }

    fun testLineMarkerWildcard_toBean_optionalMapWildcard() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var optionalMapWildcard: java.util.Optional<Map<String, *>>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isEmpty())
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardSuperA() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardSuperA: MutableMap<String, in A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcardSuperA" }
        }.size, 4)
    }

    fun testLineMarkerWildcard_toBean_mapWildcardSuperA() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardSuperA: MutableMap<String, in A>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardSuperE() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardSuperE: MutableMap<String, in E>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcardSuperE" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_mapWildcardSuperE() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardSuperE: MutableMap<String, in E>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardExtendsA() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardExtendsA: Map<String, out A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcardExtendsA" }
        }.size, 1)
    }

    fun testLineMarkerWildcard_toBean_mapWildcardExtendsA() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardExtendsA: Map<String, out A>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardExtendsE() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardExtendsE: Map<String, out E>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcardExtendsE" }
        }.size, 4)
    }

    fun testLineMarkerWildcard_toBean_mapWildcardExtendsE() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardExtendsE: Map<String, out E>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 4)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardExtendsI() {
        myFixture.configureByText(
            "FooWildcard.kt",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardExtendsI: Map<String, out I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcardExtendsI" }
        }.size, 3)
    }

    fun testLineMarkerWildcard_toBean_mapWildcardExtendsI() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapWildcardExtendsI: Map<String, out I>
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.kt", getWildcardClasses())
        myFixture.configureByText("FooWildcard.kt", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
    }


    private fun getWildcardClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean

            interface I

            @Component
            open class A : E(), I

            @Component
            open class E

            open class B : E(), I

            open class C : E(), I

            open class D : E(), I

            interface WithBeanI

            interface WithManyBeanI

            @Configuration
            open class TestConfiguration {
                @Bean
                open fun bBean(): B {
                    return B()
                }

                @Bean
                open fun dBean(): E {
                    return D()
                }

                @Bean
                open fun c_arr(): Array<C?> {
                    return arrayOfNulls(0)
                }

                @Bean
                open fun collectionObjectWithBeanI(): Collection<WithBeanI> {
                    return ArrayList()
                }

                @Bean
                open fun collectionWithManyBeanI(): Collection<WithManyBeanI> {
                    return ArrayList()
                }

                @Bean
                open fun beanSetWithManyBeanI(): Set<WithManyBeanI> {
                    return setOf()
                }

                @Bean
                open fun beanSetI(): Set<I> {
                    return setOf(A())
                }

                @Bean
                open fun beanListWithManyBeanI(): List<WithManyBeanI> {
                    return ArrayList()
                }

                @Bean
                open fun beanListI(): List<I> {
                    return listOf(A())
                }

                @Bean
                open fun beanMapStringWithBeanI(): Map<String, WithBeanI> {
                    return HashMap()
                }

                @Bean
                open fun beanMapStringI(): Map<String, I> {
                    return HashMap()
                }
            }
        """.trimIndent()
    }

}