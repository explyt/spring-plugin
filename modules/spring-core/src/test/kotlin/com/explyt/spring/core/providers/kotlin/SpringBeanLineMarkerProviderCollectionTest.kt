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

class SpringBeanLineMarkerProviderCollectionTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerCollection_toAutowired_A_listA() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listA: List<A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listA" }
        }.size, 1)
    }

    fun testLineMarkerCollection_toBean_A_listA() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listA: List<A>
                }
            """.trimIndent()

        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()


        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_collectionI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionI: Collection<I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "collectionI" }
        }.size, 3)
    }

    fun testLineMarkerCollection_toBean_collectionI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionI: Collection<I>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerCollection_toAutowired_listI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listI: List<I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listI" }
        }.size, 3)
    }

    fun testLineMarkerCollection_toBean_listI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listI: List<I>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerCollection_toAutowired_setI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setI: Set<I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setI" }
        }.size, 2)
    }

    fun testLineMarkerCollection_toBean_setI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setI: Set<I>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerCollection_toAutowired_setE() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setE: Set<E>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setE" }
        }.size, 4)
    }

    fun testLineMarkerCollection_toBean_setE() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setE: Set<E>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 4)
    }

    fun testLineMarkerCollection_toAutowired_collectionAll() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionAll: Collection<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "collectionAll" }
        }.size, 5)
    }

    fun testLineMarkerCollection_toBean_collectionAll() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionAll: Collection<*>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionObjectWithBeanI()" } }.size,
            1
        )
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionWithManyBeanI()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listAll() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listAll: List<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listAll" }
        }.size, 2)

    }

    fun testLineMarkerCollection_toBean_listAll() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listAll: List<*>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_setAll() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAll: Set<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAll" }
        }.size, 1)
    }

    fun testLineMarkerCollection_toBean_setAll() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAll: Set<*>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listsI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listsI: List<List<I>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listsI" }
        }.size, 1)

    }

    fun testLineMarkerCollection_toBean_listsI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listsI: List<List<I>>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listsC() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listsC: List<Array<C>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listsC" }
        }.size, 1)

    }

    fun testLineMarkerCollection_toBean_listsC() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listsC: List<Array<C>>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listOfMapsStringI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listOfMapsStringI: List<Map<String, I>>
                }
            """.trimIndent()
        )
        myFixture.configureByText(
            "TestConfiguration.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import java.util.*;
                
                interface I {}

                @Configuration
                open class TestConfiguration { 
                    @Bean
                    open fun beanMapStringI(): Map<String, I> {
                        return HashMap()
                    }
                }
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listOfMapsStringI" }
        }.size, 1)

    }

    fun testLineMarkerCollection_toBean_listOfMapsStringI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listOfMapsStringI: List<Map<String, I>>
                }
            """.trimIndent()
        myFixture.configureByText(
            "TestConfiguration.kt",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import java.util.*;
                
                interface I {}

                @Configuration
                open class TestConfiguration { 
                    @Bean
                    open fun beanMapStringI(): Map<String, I> {
                        return HashMap()
                    }
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_collectionWithBeanI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionWithBeanI: Collection<WithBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "collectionWithBeanI" }
        }.size, 1)

    }

    fun testLineMarkerCollection_toBean_collectionWithBeanI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionWithBeanI: Collection<WithBeanI>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionObjectWithBeanI()" } }.size,
            1
        )
    }

    fun testLineMarkerCollection_toAutowired_collectionAllWithManyBeanI() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionAllWithManyBeanI: Collection<WithManyBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "collectionAllWithManyBeanI" }
        }.size, 3)

    }

    fun testLineMarkerCollection_toBean_collectionAllWithManyBeanI() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionAllWithManyBeanI: Collection<WithManyBeanI>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionWithManyBeanI()" } }.size,
            1
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listObject() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listObject: List<Any>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listObject" }
        }.size, 2)
    }

    fun testLineMarkerCollection_toBean_listObject() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listObject: List<Any>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 0)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionObjectWithBeanI()" } }.size,
            0
        )
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionWithManyBeanI()" } }.size,
            0
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 0)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_setObject() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setObject: Set<Any>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setObject" }
        }.size, 1)

    }

    fun testLineMarkerCollection_toBean_setAllObject() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllObject: Set<Any>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 0)
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionObjectWithBeanI()" } }.size,
            0
        )
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionWithManyBeanI()" } }.size,
            0
        )
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 0)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 0)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
    }

    private fun getCollectionClasses(): String {
        return """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.stereotype.Component;

            interface I

            @Component
            open class A : E(), I

            @Component
            open class E

            class C : E(), I

            class B : E(), I

            class D : E(), I

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
                open fun arrC(): Array<C?> {
                    return arrayOfNulls(10)
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
                open fun beanListWithManyBeanI(): List<WithManyBeanI> {
                    return ArrayList()
                }

                @Bean
                open fun beanListI(): List<I> {
                    return listOf(A())
                }
            }
        """.trimIndent()
    }

}