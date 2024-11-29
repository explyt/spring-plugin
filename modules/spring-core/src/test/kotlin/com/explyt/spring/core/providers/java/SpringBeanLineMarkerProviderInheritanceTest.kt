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
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderInheritanceTest : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.javax_inject_1,
        TestLibrary.jakarta_annotation_2_1_1
    )

    fun testLineMarkerInheritance_toAutowired_I_a() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I a; 
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "a" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toBean_I_a() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I a;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_I_ab() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I ab; 
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "ab" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toBean_I_ab() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I ab; 
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toAutowired_I_c() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I c;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "c" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toBean_I_c() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I c;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toAutowired_I_e() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I e;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "e" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toBean_I_e() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired I e;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" }
        }.size, 2)
    }

    fun testLineMarkerInheritance_toAutowired_E_abe() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E abe;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "abe" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_E_abe() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E abe;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" || it == "B" || it == "E" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toAutowired_E_a() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E a;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "a" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_E_a() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E a;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "A" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_E_b() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E b;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "b" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_E_b() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired E b;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "B" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_C_c() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired C c;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerInheritance_toBean_C_c() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @org.springframework.beans.factory.annotation.Autowired C c;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.size, 0)
    }

    fun testLineMarkerInheritance_toAutowired_Inject_E_b() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @javax.inject.Inject E b;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "b" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_Inject_E_b() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @javax.inject.Inject E b;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "B" }
        }.size, 1)
    }

    fun testLineMarkerInheritance_toAutowired_Resource_E_e() {
        val vf = myFixture.copyFileToProject(
            "inheritance/TestInheritance.java"
        )
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                public class FooComponent {
                    @jakarta.annotation.Resource E e;
                }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "e" }
        }.size, 3)
    }

    fun testLineMarkerInheritance_toBean_Resource_E_e() {
        val fooComponent = """
                @org.springframework.stereotype.Component
                class FooComponent {
                    @jakarta.annotation.Resource E e;
                }
            """.trimIndent()

        myFixture.configureByText("FooComponent.java", getInheritanceClasses() + fooComponent)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it == "E" }
        }.size, 1)
    }

    private fun getInheritanceClasses(): String {
        return """
            import org.springframework.stereotype.Component;

            interface I {}

            @Component
            class E {}

            @Component
            class A extends E implements I {}

            @Component
            class B extends E implements I {}

            class C implements I {}

        """.trimIndent()
    }

}