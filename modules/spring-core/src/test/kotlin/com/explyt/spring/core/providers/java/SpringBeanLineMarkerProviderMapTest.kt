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

class SpringBeanLineMarkerProviderMapTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerMap_toAutowired_mapStringI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, I> mapStringI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<String, I> mapStringI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
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
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<Object, I> mapObjI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<Object, I> mapObjI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "mapObjectI()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapIntI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<Integer, I> mapIntI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<Integer, I> mapIntI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "mapIntegerI()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapStringWithoutBeanI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, WithoutBeanI>  mapStringWithoutBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapStringWithoutBeanI" }
        }.size, 0)
    }

    fun testLineMarkerMap_toAutowired_mapStringWithBeanI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, WithBeanI> mapStringWithBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<String, WithBeanI> mapStringWithBeanI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
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
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<ObjectExt, A> mapObjectExtA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<ObjectExt, A> mapObjectExtA;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtA()" } }.size, 1)

    }

    fun testLineMarkerMap_toAutowired_mapObjectExtWithBeanI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<ObjectExt, WithBeanI> mapObjectExtWithBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<ObjectExt, WithBeanI> mapObjectExtWithBeanI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
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
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map mapAll;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map mapAll;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
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
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<ObjectExt, I> mapObjectExtI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapObjectExtI" }
        }.size, 2)
    }

    fun testLineMarkerMap_toBean_mapObjectExtI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<ObjectExt, I> mapObjectExtI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtI()" } }.size, 1)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapObjectExtIOther()" } }.size,
            1
        )
    }

    fun testLineMarkerMap_toAutowired_mapObjectExtWithoutBeanI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<ObjectExt, WithoutBeanI>  mapObjectExtWithoutBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<ObjectExt, WithoutBeanI>  mapObjectExtWithoutBeanI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)
        assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerMap_toAutowired_mapOfListI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, java.util.List<I>> mapOfListI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<String, java.util.List<I>> mapOfListI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapArrC() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, C[]> mapArrC;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<String, C[]> mapArrC;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapOfMapStringI() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, java.util.Map<String, I>> mapOfMapStringI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
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
                    java.util.Map<String, java.util.Map<String, I>> mapOfMapStringI;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
    }

    fun testLineMarkerMap_toAutowired_mapOfStringObject() {
        myFixture.configureByText(
            "FooMap.java",
            """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, Object> mapOfStringObject;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestMap.java", getMapClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "mapOfStringObject" }
        }.size, 7)
    }

    fun testLineMarkerMap_toBean_mapOfMapStringObject() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Map<String, Object> mapOfStringObject;
                }
            """.trimIndent()

        myFixture.configureByText("FooMap.java", getMapClasses())
        myFixture.configureByText("TestMap.java", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertTrue(gutterTargetString.flatten().size >= 10)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
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
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringObject()" } }.size, 1)
    }

    private fun getMapClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean
            
            import java.util.HashMap;
            import java.util.List;
            import java.util.Map;

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
            
            class ObjectExt {}

            @Configuration
            class TestConfiguration {
                @Bean
                B bBean() {
                    return new B(); 
                }
                
                @Bean
                Map<Object, I> mapObjectI() {
                    return new HashMap<>();
                }

                @Bean
                Map<Integer, I> mapIntegerI() {
                    HashMap<Integer, I> integerIHashMap = new HashMap<>();
                    integerIHashMap.put(1, new A());
                    return integerIHashMap;
                }

                @Bean
                Map<String, WithBeanI> beanMapStringWithBeanI() {
                    return new HashMap<>();
                }

                @Bean
                Map<ObjectExt, I> beanMapObjectExtI() {
                    return new HashMap<>();
                }

                @Bean
                Map<ObjectExt, I> beanMapObjectExtIOther() {
                    return new HashMap<>();
                }

                @Bean
                Map<ObjectExt, A> beanMapObjectExtA() {
                    return new HashMap<>();
                }

                @Bean
                Map<ObjectExt, WithBeanI> beanMapObjectExtWithBeanI() {
                    return new HashMap<>();
                }

                @Bean
                List<I> beanListI() {
                    return List.of(new A());
                }

                @Bean
                C[] arrC() {
                    return new C[0];
                }

                @Bean
                Map<String, I> beanMapStringI() {
                    return new HashMap<>();
                }
                
                @Bean
                Map<String, Object> beanMapStringObject() {
                    return new HashMap<>();
                }
            }
        """.trimIndent()
    }

}