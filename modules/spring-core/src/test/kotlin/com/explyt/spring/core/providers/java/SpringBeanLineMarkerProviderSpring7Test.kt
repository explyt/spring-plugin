/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
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

        /*// To bean dependencies icon
        val depsGutters = getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBeanDependencies)
        val depsTargets = getGutterTargetString(depsGutters)
        assertTrue(depsTargets.flatten().any { it.contains("Foo", true) })
        assertTrue(depsTargets.flatten().any { it.contains("Bar", true) })
        assertTrue(depsTargets.flatten().any { it.contains("Baz", true) })*/

         val registrarBeans = SpringSearchService.getInstance(project)
             .getActiveBeansClasses(module)
             .filter { it.psiMember.isRegistrar() }
         assertTrue(registrarBeans.isNotEmpty())
         assertTrue(registrarBeans.any { it.name == "foo" })
         assertTrue(registrarBeans.any { it.name == "bar" })
         assertTrue(registrarBeans.any { it.name == "baz" })
    }
}
