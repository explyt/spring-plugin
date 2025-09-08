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
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getAllBeanGuttersByIcon
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetString
import com.explyt.util.ExplytPsiUtil.isRegistrar
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

        // To bean dependencies icon
        val depsGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val depsTargets = getGutterTargetString(depsGutters)
        assertTrue(depsTargets.flatten().any { it.contains("Foo", true) })
        assertTrue(depsTargets.flatten().any { it.contains("Bar", true) })
        assertTrue(depsTargets.flatten().any { it.contains("Baz", true) })

        val registrarBeans = SpringSearchService.getInstance(project)
            .getActiveBeansClasses(module)
            .filter { it.psiMember.isRegistrar() }
        assertTrue(registrarBeans.isNotEmpty())
        assertTrue(registrarBeans.any { it.name == "foo" })
        assertTrue(registrarBeans.any { it.name == "bar" })
        assertTrue(registrarBeans.any { it.name == "baz" })
    }
}
