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

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderSimpleTest : ExplytKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.jakarta_annotation_2_1_1
    )

    fun testLineMarkerOneBeanOneDependency_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "one_bean_one_dependency/TestConfiguration.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        TestCase.assertTrue(allBeanGutters.isNotEmpty())
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooAOneBeanOneDependency", true) }
        }.size, 1)
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooBOneBeanOneDependency", true) }
        }.size, 0)
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooCOneBeanOneDependency", true) }
        }.size, 1)
    }

    fun testLineMarkerOneBeanOneDependency_toBean() {
        val vf = myFixture.copyFileToProject(
            "one_bean_one_dependency/TestConfiguration.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        TestCase.assertTrue(allBeanGutters.isNotEmpty())
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooAOneBeanOneDependency", true) }
        }.size, 2)
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooBOneBeanOneDependency", true) }
        }.size, 0)
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooCOneBeanOneDependency", true) }
        }.size, 1)
    }

    fun testLineMarkerOneBeanTwoDependency_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "one_bean_two_dependency/TestConfiguration.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBean }
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        TestCase.assertTrue(allBeanGutters.isNotEmpty())
        for (gutter in gutterTargetString) {
            TestCase.assertEquals(gutter.filter { it.contains("foo", true) }.size, 2)
        }
    }

    fun testLineMarkerOneBeanTwoDependency_toBean() {
        val vf = myFixture.copyFileToProject(
            "one_bean_two_dependency/TestConfiguration.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        TestCase.assertTrue(allBeanGutters.isNotEmpty())

        for (gutter in gutterTargetString) {
            TestCase.assertEquals(gutter.filter { it.contains("foo", true) }.size, 1)
        }
    }

    fun testLineMarker_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "autowired/FooFormatter.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBean }
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        TestCase.assertTrue(allBeanGutters.isNotEmpty())
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooFormatter", true) }
        }.size, 3)
    }

    fun testLineMarker_toBean() {
        val vf = myFixture.copyFileToProject(
            "autowired/FooFormatter.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        val gutterTargetString = allBeanGutters.asSequence()
            .map { SpringGutterTestUtil.getGutterTargetsStrings(it) }
            .filter { it.isNotEmpty() }
            .toList()

        TestCase.assertTrue(allBeanGutters.isNotEmpty())
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooFormatter", true) }
        }.size, 3)
    }

    fun testLineMarkerByBeanName_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "method_navigates/TestConfiguration.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString
            .filter { it.size == 1 }
            .flatMap { gutter ->
                gutter.filter {
                    it.contains("namedBeanFoo", true)
                            || it.contains("foo", true)
                }
            }.size,
            2
        )
    }

    fun testLineMarkerByBeanName_toBean() {
        val vf = myFixture.copyFileToProject(
            "method_navigates/TestConfiguration.kt"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter {
                it.contains("namedBeanFoo", true)
                        || it.contains("foo", true)
            }
        }.size, 5)
    }

    fun testLineMarkerResource_toAutowired() {
        myFixture.configureByText(
            "FooComponent.kt",
            """
                @org.springframework.stereotype.Component
                internal class E {}

                @org.springframework.stereotype.Component
                internal class Foo {
                    @jakarta.annotation.Resource
                    private lateinit var e: E
                }
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "e" } }.size, 1
        )
    }

    fun testLineMarkerResource_toBean() {
        myFixture.configureByText(
            "FooComponent.kt",
            """
                @org.springframework.stereotype.Component
                internal class E {}

                @org.springframework.stereotype.Component
                internal class Foo {
                    @jakarta.annotation.Resource
                    private lateinit var e: E
                }
            """.trimIndent()
        )
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1
        )
    }

    fun testLineMarker_BeanDeclaration() {
        myFixture.configureByText(
            "FooComponent.kt",
            """
                class Foo

                @${SpringCoreClasses.CONFIGURATION}
                open class FooComponent {
                
                    @${SpringCoreClasses.BEAN}
                    fun foo():Foo {
                        return Foo()
                    }                                    
                }

            """.trimIndent()
        )
        myFixture.doHighlighting()

        val allBeanGutters = SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = SpringGutterTestUtil.getGutterTargetString(allBeanGutters)

        TestCase.assertTrue(gutterTargetString.flatten().all { it == "foo()" })
    }

    fun testNoLineMarkerOnConstructorParam() {
        val person = """                
                class Person(val name:String)
            """.trimIndent()

        myFixture.configureByText("Person.kt", person)
        myFixture.doHighlighting()

        val allBeanGutters = myFixture.findAllGutters()
            .filter { it.icon == SpringIcons.SpringBeanDependencies }
        assertTrue(allBeanGutters.isEmpty())
    }
}