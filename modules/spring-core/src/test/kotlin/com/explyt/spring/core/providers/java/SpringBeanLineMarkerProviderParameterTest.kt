/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderParameterTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
    )

    fun testLineMarkerParameterEA_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(E fooEParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(A fooAParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(E fooEParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(A fooAParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(C fooEParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameter_toAutowired() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(A fooAParameter, E fooEParameter,C fooCParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired 
                    void test(
                        A fooAParameter, 
                        E fooEParameter, 
                        C fooCParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooConstructorA {
                    private final A a;
                    Foo(A fooConstructorParameter) { this.a = fooConstructorParameter; } 
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter ->
                gutter.filter { it.contains("fooConstructorParameter", true) }
            }.size, 1
        )
    }

    fun testLineMarkerParameterAInConstructor_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                class FooConstructorA {
                    private final A a;
                    Foo(A fooConstructorParameter) { this.a = fooConstructorParameter; }
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooConstructorC {
                    private final C c;
                    Foo(C fooConstructorParameter) { this.c = fooConstructorParameter; } 
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterCInConstructor_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                class FooConstructorC {
                    private final C c;
                    Foo(C fooConstructorParameter) { this.c = fooConstructorParameter; }
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooOpt {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optA(java.util.Optional<A> aFooOptionalParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void optE(java.util.Optional<E> eFooOptionalParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooOpt {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optA(java.util.Optional<A> aFooOptionalParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void optE(java.util.Optional<E> eFooOptionalParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooOptC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optC(Optional<C> cFooOptionalParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooOptC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                class FooOptC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void optC(Optional<C> cFooOptionalParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapA(java.util.Map<String, A> mapAParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapE(java.util.Map<String, E> mapEParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooMap {
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapA(java.util.Map<String, A> mapAParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapE(java.util.Map<String, E> mapEParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooMapC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapC(java.util.Map<String, C> mapCParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooMapC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                class FooMapC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapC(java.util.Map<String, C> mapCParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooList {
                    @org.springframework.beans.factory.annotation.Autowired
                    void listA(java.util.List<A> listAParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void listE(java.util.List<E> listEParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooList {
                    @org.springframework.beans.factory.annotation.Autowired
                    void listA(java.util.List<A> listAParameter) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void mapE(java.util.List<E> listEParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooListC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void listC(java.util.List<C> listCParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerParameterFooListC_toBean() {
        val fooComponent =
            """
                @org.springframework.stereotype.Component
                class FooListC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void listC(java.util.List<C> listCParameter) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    void arrayA(A[] arrA) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void arrayE(E[] arrE) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooArray {
                    @org.springframework.beans.factory.annotation.Autowired
                    void arrayA(A[] arrA) {}
                    
                    @org.springframework.beans.factory.annotation.Autowired
                    void arrayE(E[] arrE) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                class FooArrayC {
                    @org.springframework.beans.factory.annotation.Autowired
                    void arrayC(C[] arrC) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
                    void arrayC(C[] arrC) {}
                }
            """.trimIndent()
        myFixture.configureByText("FooComponent.java", getParameterClasses() + fooComponent)
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
            class E {}

            @Component
            class A extends E {}

            class C extends E {}
            
        """.trimIndent()
    }

}
