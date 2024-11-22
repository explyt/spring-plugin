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
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<A> listA;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.List<A> listA;
                }
            """.trimIndent()

        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
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
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Collection<I> collectionI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.Collection<I> collectionI; 
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerCollection_toAutowired_listI() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<I> listI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.List<I> listI;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerCollection_toAutowired_setI() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<I> setI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.Set<I> setI;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerCollection_toAutowired_setE() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<E> setE;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.Set<E> setE;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 4)
    }

    fun testLineMarkerCollection_toAutowired_collectionAll() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Collection collectionAll;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.Collection collectionAll;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
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
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List listAll;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.List listAll;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_setAll() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set setAll;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.Set setAll;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listsI() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<java.util.List<I>> listsI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.List<java.util.List<I>> listsI;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listsC() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<C[]> listsC;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.List<C[]> listsC;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_listOfMapsStringI() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                import java.util.*;
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    List<Map<String, I>> listOfMapsStringI;
                }
            """.trimIndent()
        )
        myFixture.configureByText(
            "TestConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import java.util.*;
                
                interface I {}

                @Configuration
                class TestConfiguration { 
                    @Bean
                    Map<String, I> beanMapStringI() { return new HashMap<>(); }
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
                import java.util.*;
                
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    List<Map<String, I>> listOfMapsStringI;
                }
            """.trimIndent()
        myFixture.configureByText(
            "TestConfiguration.java",
            """
                import org.springframework.context.annotation.Bean;
                import org.springframework.context.annotation.Configuration;
                import java.util.*;
                
                interface I {}

                @Configuration
                class TestConfiguration { 
                    @Bean
                    Map<String, I> beanMapStringI() { return new HashMap<>(); }
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanMapStringI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_collectionWithBeanI() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Collection<WithBeanI> collectionWithBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
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
                    java.util.Collection<WithBeanI> collectionWithBeanI;
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.java", getCollectionClasses())
        myFixture.configureByText("TestCollection.java", fooCollection)
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
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Collection<WithManyBeanI> collectionAllWithManyBeanI;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "collectionAllWithManyBeanI" }
        }.size, 3)

    }

    fun testLineMarkerCollection_toAutowired_objectList() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<Object> objectList;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertTrue(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "objectList" }
        }.size >= 10)
    }

    fun testLineMarkerCollection_toBean_objectList() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.List<Object> objectList;
                }
            """.trimIndent()
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
        myFixture.configureByText("FooCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "b()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "d()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
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

    fun testLineMarkerCollection_toAutowired_objectSet() {
        myFixture.configureByText(
            "FooCollection.java",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<Object> objectSet;
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertTrue(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "objectSet" }
        }.size >= 10)

    }

    fun testLineMarkerCollection_toBean_setAllObject() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    java.util.Set<Object> objectSet;
                }
            """.trimIndent()
        myFixture.configureByText("TestCollection.java", getCollectionClasses())
        myFixture.configureByText("FooCollection.java", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "b()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "d()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 1)
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

    private fun getCollectionClasses(): String {
        return """
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.stereotype.Component;

            import java.util.ArrayList;
            import java.util.Collection;
            import java.util.List;
            import java.util.Set;
            
            interface I {}

            @Component
            class E {}

            @Component
            class A extends E implements I {}

            class C extends E implements I {}

            class B extends E implements I {}

            class D extends E implements I {}

            interface WithBeanI {}

            interface WithManyBeanI {}

            @Configuration
            class TestConfiguration {
                @Bean
                B b() { return new B(); }

                @Bean
                E d() { return new D(); }

                @Bean
                C[] arrC() { return new C[5]; }

                @Bean
                Collection<WithBeanI> collectionObjectWithBeanI() { return new ArrayList<>(); }

                @Bean
                Collection<WithManyBeanI> collectionWithManyBeanI() { return new ArrayList<>(); }

                @Bean
                Set<WithManyBeanI> beanSetWithManyBeanI() { return Set.of(); }

                @Bean
                List<WithManyBeanI> beanListWithManyBeanI() { return new ArrayList<>(); }

                @Bean
                List<I> beanListI() { return List.of(new A()); }
            }
        """.trimIndent()
    }

}