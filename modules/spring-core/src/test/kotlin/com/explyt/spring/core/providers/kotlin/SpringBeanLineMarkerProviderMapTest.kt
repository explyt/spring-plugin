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

class SpringBeanLineMarkerProviderMapTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerMap_toAutowired_mapStringI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapStringI: Map<String, I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapStringI" }
        }.size, 3)
    }

    fun testLineMarkerMap_toBean_mapStringI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapStringI: Map<String, I>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapObjI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjI: Map<Any, I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapObjI" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapObjI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjI: Map<Any, I>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "mapObjectI()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapIntI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapIntI: Map<Int, I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapIntI" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapIntI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapIntI: Map<Int, I>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "mapIntegerI()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapStringWithoutBeanI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapStringWithoutBeanI: Map<String, WithoutBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapStringWithoutBeanI" }
        }.size, 0)
    }

    fun testLineMarkerMap_toBean_mapStringWithoutBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapStringWithoutBeanI: Map<String, WithoutBeanI>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isNotEmpty())
    }

    fun testLineMarkerMap_toAutowired_mapStringWithBeanI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapStringWithBeanI: Map<String, WithBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapStringWithBeanI" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapStringWithBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapStringWithBeanI: Map<String, WithBeanI>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringWithBeanI()" } }.size,
            1
        )
    }

    fun testLineMarkerMap_toAutowired_mapObjectExtA() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtA: Map<ObjectExt, A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapObjectExtA" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapObjectExtA() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtA: Map<ObjectExt, A>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtA()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapObjectExtWithBeanI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtWithBeanI: Map<ObjectExt, WithBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapObjectExtWithBeanI" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapObjectExtWithBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtWithBeanI: Map<ObjectExt, WithBeanI>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtWithBeanI()" } }.size,
            1
        )

    }

    fun testLineMarkerMap_toAutowired_mapAll() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapAll: Map<*, *>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapAll" }
        }.size, 9)
    }

    fun testLineMarkerMap_toBean_mapAll() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapAll: Map<*, *>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "mapObjectI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "mapIntegerI()" } }.size, 1)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringWithBeanI()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtI()" } }.size, 1)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtIOther()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtA()" } }.size, 1)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtWithBeanI()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapObjectExtI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtI: Map<ObjectExt, I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapObjectExtI" }
        }.size, 3)
    }

    fun testLineMarkerMap_toBean_mapObjectExtI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtI: Map<ObjectExt, I>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtI()" } }.size, 1)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtIOther()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtA()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapObjectExtWithoutBeanI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtWithoutBeanI: Map<ObjectExt, WithoutBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapObjectExtWithoutBeanI" }
        }.size, 0)
    }

    fun testLineMarkerMap_toBean_mapObjectExtWithoutBeanI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapObjectExtWithoutBeanI: Map<ObjectExt, WithoutBeanI>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)
        assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerMap_toAutowired_mapOfListI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapOfListI: Map<String, List<I>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapOfListI" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapOfListI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapOfListI: Map<String, List<I>>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapArrC() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapArrC: Map<String, Array<C>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapArrC" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapArrC() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapArrC: Map<String, Array<C>>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapOfMapStringI() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapOfMapStringI: Map<String, Map<String, I>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapOfMapStringI" }
        }.size, 1)
    }

    fun testLineMarkerMap_toBean_mapOfMapStringI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var mapOfMapStringI: Map<String, Map<String, I>>
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.kt", getMapClasses())
        myFixture.configureByText("TestMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapOfStringObject() {
        myFixture.configureByText(
            "FooMap.kt",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var objectMap: Map<String, Any>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "objectMap" } }.size, 3)
    }

    fun testLineMarkerMap_toBean_mapOfMapStringObject() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var objectMap: Map<String, Any>
                }
            """.trimIndent()

        myFixture.configureByText("TestMap.kt", getMapClasses())
        myFixture.configureByText("FooMap.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 3)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringWithBeanI()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringObject()" } }.size, 1)
    }

    private fun getMapClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean
            import java.util.HashMap
            
            interface I

            @Component
            open class E

            @Component
            class A : E(), I

            class B : E(), I

            class C : E(), I

            class D : E(), I

            class ObjectExt

            interface WithBeanI

            interface WithoutBeanI

            @Configuration
            open class TestConfiguration {
                @Bean
                open fun bBean(): B {
                    return B()
                }

                @Bean
                open fun mapObjectI(): Map<Any, I> {
                    return HashMap()
                }
    
                @Bean
                open fun mapIntegerI(): Map<Int, I> {
                    val integerIHashMap = HashMap<Int, I>()
                    integerIHashMap[1] = A()
                    return integerIHashMap
                }

                @Bean
                open fun beanMapStringWithBeanI(): Map<String, WithBeanI> {
                    return HashMap()
                }

                @Bean
                open fun beanMapObjectExtI(): Map<ObjectExt, I> {
                    return HashMap()
                }

                @Bean
                open fun beanMapObjectExtIOther(): Map<ObjectExt, I> {
                    return HashMap()
                }

                @Bean
                open fun beanMapObjectExtA(): Map<ObjectExt, A> {
                    return HashMap()
                }

                @Bean
                open fun beanMapObjectExtWithBeanI(): Map<ObjectExt, WithBeanI> {
                    return HashMap()
                }

                @Bean
                open fun beanListI(): List<I> {
                    return listOf(A())
                }

                @Bean
                open fun arrC(): Array<C?> {
                    return arrayOfNulls(0)
                }
        
                @Bean
                open fun beanMapStringI(): Map<String, I> {
                    return HashMap()
                }
                
                @Bean
                open fun beanMapStringObject(): Map<String, Any> {
                    return HashMap()
                }
            }
        """.trimIndent()
    }

}