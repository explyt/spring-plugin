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
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderParameterTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerParameterEA_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooParameter {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooEParameter: E) {} 

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooAParameter: A) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooEParameter" } }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooAParameter" } }.size, 1
        )
    }

    fun testLineMarkerParameterEA_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooParameter {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooEParameter: E) {} 

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooAParameter: A) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("E", true) }.toSet()
            }.size, 1
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it.contains("A", true) }.toSet() }.size, 2
        )
    }

    fun testLineMarkerParameterC_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooParameter {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooCParameter: C) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooParameter {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooCParameter: C) {} 
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        TestCase.assertEquals(allBeanGutters.size, 0)
    }

    fun testLineMarkerParameter_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(fooAParameter: A, fooEParameter: E, fooCParameter: C) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        gutterTargetString.map { gutter ->
            TestCase.assertEquals(gutter.filter { it == "fooEParameter" }.size, 1)
        }
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "fooAParameter" } }.size, 1
        )
    }

    fun testLineMarkerParameter_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun test(
                        fooAParameter: A, 
                        fooEParameter: E, 
                        fooCParameter: C) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        gutterTargetString.map { gutter ->
            TestCase.assertEquals(gutter.filter { it == "A" }.size, 1)
        }
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1
        )
    }

    fun testLineMarkerParameterAInConstructor_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooConstructorA(private val a: A)
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("a", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterAInConstructor_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooConstructorA(private val a: A)
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("A", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterCInConstructor_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooConstructorC(private val c: C)
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterCInConstructor_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooConstructorC(private val c: C)
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooOpt_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooOpt {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun optA(aFooOptionalParameter: java.util.Optional<A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun optE(eFooOptionalParameter: java.util.Optional<E>) {}
                }            
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("eFooOptionalParameter", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("aFooOptionalParameter", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooOpt_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooOpt {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun optA(aFooOptionalParameter: java.util.Optional<A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun optE(eFooOptionalParameter: java.util.Optional<E>) {}
                }            
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("A", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("E", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooOptC_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooOptC {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun optC(cFooOptionalParameter: Optional<C>) {}
                }            
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooOptC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooOptC {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun optC(cFooOptionalParameter: Optional<C>) {}
                }            
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooMap_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun mapA(mapAParameter: java.util.Map<String, A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun mapE(mapEParameter: java.util.Map<String, E>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("mapEParameter", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("mapAParameter", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooMap_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun mapA(mapAParameter: java.util.Map<String, A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun mapE(mapEParameter: java.util.Map<String, E>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("A", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("E", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooMapC_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooMapC {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun mapC(mapCParameter: java.util.Map<String, C>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooMapC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooMapC {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun mapC(mapCParameter: java.util.Map<String, C>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooList_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooList {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun listA(listAParameter: java.util.List<A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun listE(listEParameter: java.util.List<E>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("listEParameter", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("listAParameter", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooList_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooList {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun listA(listAParameter: java.util.List<A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun listE(listEParameter: java.util.List<E>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("A", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("E", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooListC_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooListC {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun listC(listAParameter: java.util.List<C>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooListC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooListC {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun listC(listAParameter: java.util.List<C>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooArray_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun arrayA(arrA: Array<A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun arrayE(arrE: Array<E>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("arrE", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("arrA", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooArray_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired 
                    fun arrayA(arrA: Array<A>) {}

                    @org.springframework.beans.factory.annotation.Autowired 
                    fun arrayE(arrE: Array<E>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("A", true) }
            }.size, 2
        )
        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("E", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterFooArrayC_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                internal class FooArrayC {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun arrayC(arrC: Array<C>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooArrayC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                class FooArrayC {
                    @org.springframework.beans.factory.annotation.Autowired
                    fun arrayC(arrC: Array<C>) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.kt", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    private fun getParameterClasses(): String {
        return """
            import org.springframework.stereotype.Component;

            @Component
            internal open class E

            @Component
            internal class A : E()

            internal class C : E()
            
        """.trimIndent()
    }
}