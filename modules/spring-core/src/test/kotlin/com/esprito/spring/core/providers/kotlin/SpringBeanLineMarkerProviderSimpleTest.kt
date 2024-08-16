package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringCoreClasses
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import com.esprito.spring.test.util.SpringGutterTestUtil
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderSimpleTest : EspritoKotlinLightTestCase() {
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
}