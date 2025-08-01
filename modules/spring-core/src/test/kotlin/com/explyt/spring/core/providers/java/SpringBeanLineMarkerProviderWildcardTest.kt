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

class SpringBeanLineMarkerProviderWildcardTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerWildcard_toAutowired_setAllWildcard() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<?> setAllWildcard;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Set<?> setAllWildcard;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetI()" } }.size, 1)
    }

    fun _testLineMarkerWildcard_toAutowired_setAllWildcardSuperA() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? super A> setAllWildcardSuperA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 5
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardSuperA" }
        }.size, 3)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardSuperA() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? super A> setAllWildcardSuperA;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardSuperE() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? super E> setAllWildcardSuperE;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardSuperE" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardSuperE() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? super E> setAllWildcardSuperE;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardSuperI() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? super I> setAllWildcardSuperI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardSuperI" }
        }.size, 1)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardSuperI() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? super I> setAllWildcardSuperI;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "beanSetI()" }
        }.size, 1)

    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardExtendsA() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? extends A> setAllWildcardExtendsA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Set<? extends A> setAllWildcardExtendsA;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_setAllWildcardExtendsE() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? extends E> setAllWildcardExtendsE;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Set<? extends E> setAllWildcardExtendsE;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
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

    fun _testLineMarkerWildcard_toAutowired_setAllWildcardExtendsI() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? extends I> setAllWildcardExtendsI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 4
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllWildcardExtendsI" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_setAllWildcardExtendsI() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<? extends I> setAllWildcardExtendsI;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
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
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ?> mapWildcard;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcard" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_mapWildcard() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ?> mapWildcard;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
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
            "FooWildcard.java",
            """
                import java.util.Optional;
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<java.util.Map<String, ?>> optionalMapWildcard;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "optionalMapWildcard" }
        }.size, 0)
    }

    fun testLineMarkerWildcard_toBean_optionalMapWildcard() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Optional<java.util.Map<String, ?>> optionalMapWildcard;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardSuperA() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ? super A> mapWildcardSuperA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Map<String, ? super A> mapWildcardSuperA;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardSuperE() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ? super E> mapWildcardSuperE;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Map<String, ? super E> mapWildcardSuperE;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
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
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ? extends A> mapWildcardExtendsA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Map<String, ? extends A> mapWildcardExtendsA;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
    }

    fun testLineMarkerWildcard_toAutowired_mapWildcardExtendsE() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ? extends E> mapWildcardExtendsE;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
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
                    java.util.Map<String, ? extends E> mapWildcardExtendsE;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
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

    fun _testLineMarkerWildcard_toAutowired_mapWildcardExtendsI() {
        myFixture.configureByText(
            "FooWildcard.java",
            """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ? extends I> mapWildcardExtendsI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 3 !!!!!!!
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapWildcardExtendsI" }
        }.size, 2)
    }

    fun testLineMarkerWildcard_toBean_mapWildcardExtendsI() {
        val fooWildcard = """
                @org.springframework.stereotype.Component
                class FooWildcard {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, ? extends I> mapWildcardExtendsI;
                }
            """.trimIndent()

        myFixture.configureByText("TestWildcard.java", getWildcardClasses())
        myFixture.configureByText("FooWildcard.java", fooWildcard)
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
            
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;
            import java.util.Set;

            interface I {}

            @Component
            class A extends E implements I {}

            @Component
            class E {}

            class B extends E implements I {}

            class C extends E implements I {}

            class D extends E implements I {}

            interface WithBeanI {}

            interface WithManyBeanI {}

            @Configuration
            class TestConfiguration {
                @Bean 
                B bBean() { return new B(); }
                
                @Bean 
                E dBean() { return new D(); }
                
                @Bean 
                C[] c_arr() { return new C[0]; }

                @Bean 
                Collection<WithBeanI> collectionObjectWithBeanI() { return new ArrayList<>(); }
                
                @Bean 
                Collection<WithManyBeanI> collectionWithManyBeanI() { return new ArrayList<>(); }
                
                @Bean 
                Set<WithManyBeanI> beanSetWithManyBeanI() { return Set.of(); }
                
                @Bean
                Set<I> beanSetI() { return Set.of(new A()); }
                
                @Bean
                List<WithManyBeanI> beanListWithManyBeanI() { return new ArrayList<>(); }
                
                @Bean 
                List<I> beanListI() { return List.of(new A()); }
                
                @Bean 
                Map<String, WithBeanI> beanMapStringWithBeanI() { return new HashMap<>(); }
                
                @Bean 
                Map<String, I> beanMapStringI() { return new HashMap<>(); }
            }
        """.trimIndent()
    }

}