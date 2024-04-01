package com.esprito.spring.core.providers.java

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.esprito.spring.core.util.SpringGutterTestUtil.getGutterTargetString
import com.esprito.spring.test.EspritoJavaLightTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderSimpleTest : EspritoJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.jakarta_annotation_2_1_1
    )

    fun testLineMarkerOneBeanOneDependency_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "one_bean_one_dependency/TestConfiguration.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooAOneBeanOneDependency", true) }
        }.size, 2)  // !!expected 1
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooBOneBeanOneDependency", true) }
        }.size, 0)
        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooCOneBeanOneDependency", true) }
        }.size, 1)
    }

    fun testLineMarkerOneBeanOneDependency_toBean() {
        val vf = myFixture.copyFileToProject(
            "one_bean_one_dependency/TestConfiguration.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

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

    fun testLineMarkerOneBeanTwoDependency_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "one_bean_two_dependency/TestConfiguration.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        for (gutter in gutterTargetString) {
            TestCase.assertEquals(gutter.filter { it.contains("foo", true) }.size, 2)
        }
    }

    fun testLineMarkerOneBeanTwoDependency_toBean() {
        val vf = myFixture.copyFileToProject(
            "one_bean_two_dependency/TestConfiguration.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        for (gutter in gutterTargetString) {
            TestCase.assertEquals(gutter.filter { it.contains("foo", true) }.size, 1)
        }
    }

    fun testLineMarker_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "autowired/FooFormatter.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooFormatter", true) }
        }.size, 3)
    }

    fun testLineMarker_toBean() {
        val vf = myFixture.copyFileToProject(
            "autowired/FooFormatter.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter { it.contains("fooFormatter", true) }
        }.size, 3)
    }

    fun testLineMarkerByBeanName_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "method_navigates/TestConfiguration.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val icons = setOf(SpringIcons.SpringBean, SpringIcons.springBeanInactive)
        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, icons)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

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
            "method_navigates/TestConfiguration.java"
        )
        myFixture.configureFromExistingVirtualFile(vf)
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(gutterTargetString.flatMap { gutter ->
            gutter.filter {
                it.contains("namedBeanFoo", true)
                        || it.contains("foo", true)
            }
        }.size, 2)
    }

    fun testLineMarkerResource_toAutowired() {
        myFixture.configureByText(
            "FooComponent.java",
            """
                @org.springframework.stereotype.Component
                class E {}

                @org.springframework.stereotype.Component
                class Foo {
                    @jakarta.annotation.Resource E e;  /** Target {@link E} */
                }

            """.trimIndent()
        )
        myFixture.doHighlighting()

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

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

        val allBeanGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val gutterTargetString = getGutterTargetString(allBeanGutters)

        TestCase.assertEquals(
            gutterTargetString.flatMap { gutter -> gutter.filter { it == "E" } }.size, 1
        )
    }
}