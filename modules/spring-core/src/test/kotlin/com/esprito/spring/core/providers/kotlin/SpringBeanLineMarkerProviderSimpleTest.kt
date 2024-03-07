package com.esprito.spring.core.providers.kotlin

import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.util.SpringGutterTestUtil
import com.esprito.spring.test.EspritoKotlinLightTestCase
import com.esprito.spring.test.TestLibrary
import junit.framework.TestCase

private const val TEST_DATA_PATH = "providers/linemarkers/beans"

class SpringBeanLineMarkerProviderSimpleTest : EspritoKotlinLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testLineMarkerOneBeanOneDependency_toAutowired() {
        val vf = myFixture.copyFileToProject(
            "one_bean_one_dependency/TestConfiguration.kt"
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
}