package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.esprito.spring.core.util.SpringGutterTestUtil.getGutterTargetString
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary

class SpringBeanLineMarkerProviderCollectionTest : EspritoKotlinLightTestCase() {
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
                    lateinit var collectionI: java.util.Collection<I>
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
                    lateinit var collectionI: java.util.Collection<I>
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
                    lateinit var listI: java.util.List<I>
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
                    lateinit var listI: java.util.List<I>
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
                    lateinit var setI: java.util.Set<I>
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
                    lateinit var setI: java.util.Set<I>
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
                    lateinit var setE: java.util.Set<E>
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
                    lateinit var setE: java.util.Set<E>
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

    fun testLineMarkerCollection_toBean_collectionAll() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionAll: java.util.Collection<*>
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

    fun testLineMarkerCollection_toBean_listAll() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listAll: java.util.List<*>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toBean_setAll() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAll: java.util.Set<*>
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
                    lateinit var listsI: java.util.List<java.util.List<I>>
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
                    lateinit var listsI: java.util.List<java.util.List<I>>
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
                    lateinit var listsC: java.util.List<Array<C>>
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
                    lateinit var listsC: java.util.List<Array<C>>
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
                    lateinit var listOfMapsStringI: java.util.List<java.util.Map<String, I>>
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
                import java.util.*;
                
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
                    lateinit var collectionWithBeanI: java.util.Collection<WithBeanI>
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
                    lateinit var collectionWithBeanI: java.util.Collection<WithBeanI>
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
                    lateinit var collectionAllWithManyBeanI: java.util.Collection<WithManyBeanI>
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
                    lateinit var collectionAllWithManyBeanI: java.util.Collection<WithManyBeanI>
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

    // problems with tests

    fun testLineMarkerCollection_toAutowired_objects() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var objects: java.util.List<Any>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 2 bean
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "objects" }
        }.size, 4)

    }

    fun testLineMarkerCollection_toBean_objects() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var objects: java.util.List<Any>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 0)
        // TODO: should be 0 bean
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionObjectWithBeanI()" } }.size,
            1
        )
        // TODO: should be 0 bean
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionWithManyBeanI()" } }.size,
            1
        )
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_setAllObject() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllObject: java.util.Set<Any>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAllObject" }
        }.size, 4)

    }

    fun testLineMarkerCollection_toBean_setAllObject() {
        val fooCollection = """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAllObject: java.util.Set<Any>
                }
            """.trimIndent()
        myFixture.configureByText("FooCollection.kt", getCollectionClasses())
        myFixture.configureByText("TestCollection.kt", fooCollection)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "A" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "bBean()" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "dBean()" } }.size, 1)
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "arrC()" } }.size, 0)
        // TODO: should be 0 bean
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionObjectWithBeanI()" } }.size,
            1
        )
        // TODO: should be 0 bean
        assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "collectionWithManyBeanI()" } }.size,
            1
        )
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanSetWithManyBeanI()" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListWithManyBeanI()" } }.size, 1)
        // TODO: should be 0 bean
        assertEquals(gutterTargetString.flatMap { gutter -> gutter.filter { it == "beanListI()" } }.size, 1)
    }

    fun testLineMarkerCollection_toAutowired_collectionAll() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var collectionAll: java.util.Collection<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 5 bean
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "collectionAll" }
        }.size, 10)

    }

    fun testLineMarkerCollection_toAutowired_listAll() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var listAll: java.util.List<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 2 bean
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "listAll" }
        }.size, 7)

    }

    fun testLineMarkerCollection_toAutowired_setAll() {
        myFixture.configureByText(
            "FooCollection.kt",
            """
                @org.springframework.stereotype.Component
                class FooCollection {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var setAll: java.util.Set<*>
                }
            """.trimIndent()
        )
        myFixture.configureByText("TestCollection.kt", getCollectionClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // TODO: should be 1 bean
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "setAll" }
        }.size, 6)

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