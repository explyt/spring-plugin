/*
 * Copyright © 2025 Explyt Ltd
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
import org.jetbrains.kotlin.test.TestMetadata


private const val TEST_DATA_PATH = "providers/linemarkers/beans/spring7"

@TestMetadata(TEST_DATA_PATH)
class SpringBeanLineMarkerProviderSpring7Test : ExplytJavaLightTestCase() {
    override fun getTestDataPath(): String = super.getTestDataPath() + TEST_DATA_PATH

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.jakarta_annotation_2_1_1
    )

    fun testLineMarkers_forBeanRegistrar() {
        val vf = myFixture.copyDirectoryToProject(".", "")
        val file = vf.findChild("com")!!.findChild("app")!!.findChild("User.java")!!
        myFixture.configureFromExistingVirtualFile(file)
        myFixture.doHighlighting()

        // All gutters just to ensure there is some
        val all = myFixture.findAllGutters()
        assertFalse("No gutters found at all", all.isEmpty())

        // To autowired icon
        val autowiredGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        val autowiredTargets = getGutterTargetString(autowiredGutters)
        // expect fields foo, bar, baz each to have one gutter
        TestCase.assertEquals(1, autowiredTargets.flatten().count { it.contains("foo", true) })
        TestCase.assertEquals(1, autowiredTargets.flatten().count { it.contains("bar", true) })
        TestCase.assertEquals(1, autowiredTargets.flatten().count { it.contains("baz", true) })

        // To bean dependencies icon
        val depsGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val depsTargets = getGutterTargetString(depsGutters)
        TestCase.assertTrue(depsTargets.flatten().any { it.contains("Foo", true) })
        TestCase.assertTrue(depsTargets.flatten().any { it.contains("Bar", true) })
        TestCase.assertTrue(depsTargets.flatten().any { it.contains("Baz", true) })
    }
}
