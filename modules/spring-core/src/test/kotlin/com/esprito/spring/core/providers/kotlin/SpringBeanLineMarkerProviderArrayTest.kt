package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.esprito.spring.core.util.SpringGutterTestUtil.getGutterTargetString
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary

class SpringBeanLineMarkerProviderArrayTest : EspritoJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerOptional_toAutowired_A_arrayA() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayA: Array<A>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                    lateinit var arrayA: Array<A>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun _testLineMarkerOptional_toAutowired_arrayE() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayE: Array<E>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 6 beans
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayE" }
        }.size, 4)
    }

    fun _testLineMarkerOptional_toBean_arrayE() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayE: Array<E>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 2 beans
        assertEquals(gutterTargetString.flatten().size, 4)
    }

    fun testLineMarkerOptional_toAutowired_arrayB() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayB: Array<B>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                    lateinit var arrayB: Array<B>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayD() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayD: Array<D>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                    lateinit var arrayD: Array<D>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isEmpty())
    }

    fun _testLineMarkerOptional_toAutowired_arrayI() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayI: Array<I>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        // now show 4 beans
        assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "arrayI" }
        }.size, 2)
    }

    fun testLineMarkerOptional_toBean_arrayI() {
        val fooOptional = """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayI: Array<I>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 2)
    }

    fun testLineMarkerOptional_toAutowired_beanArrayC() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayC: Array<C>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                    lateinit var arrayC: Array<C>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_list_beanArrayC() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayC: java.util.List<Array<C>>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                        lateinit var arrayC: java.util.List<Array<C>>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayWithBeanI() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayWithBeanI: Array<WithBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                    lateinit var arrayWithBeanI: Array<WithBeanI>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBeanDependencies)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        assertEquals(gutterTargetString.flatten().size, 1)
    }

    fun testLineMarkerOptional_toAutowired_arrayWithoutBeanI() {
        myFixture.configureByText(
            "FooArray.kt",
            """
                @org.springframework.stereotype.Component
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    lateinit var arrayWithoutBeanI: Array<WithoutBeanI>
                }
            """.trimIndent()
        )
        myFixture.configureByText("ArrayOptional.kt", getArrayClasses())
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
                    lateinit var arrayWithoutBeanI: Array<WithoutBeanI>
                }
            """.trimIndent()

        myFixture.configureByText("FooArray.kt", getArrayClasses())
        myFixture.configureByText("TestArray.kt", fooOptional)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isEmpty())
    }

    private fun getArrayClasses(): String {
        return """
            import org.springframework.stereotype.Component;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Bean

            interface I

            @Component
            open class E

            @Component
            class A : E(), I

            class B : E(), I

            class C : E(), I

            class D : E(), I
            
            interface WithBeanI

            interface WithoutBeanI

            @Configuration
            open class TestConfiguration {
                @Bean
                fun b(): B {
                    return B()
                }

                @Bean
                fun d(): E {
                    return D()
                }

                @Bean
                fun arrayA(): Array<A?> {
                    return arrayOfNulls(1)
                }

                @Bean
                fun arrayC(): Array<C?> {
                    return arrayOfNulls(2)
                }

                @Bean
                fun beanArrayWithBeanI(): Array<WithBeanI?> {
                    return arrayOfNulls(50)
                }
            }
        """.trimIndent()
    }

}