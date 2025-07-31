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

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiMethod
import junit.framework.TestCase
import org.jetbrains.kotlin.idea.util.projectStructure.getModule

class SpringSearchServiceTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testImportComponent() {
        val virtualFile = myFixture.copyDirectoryToProject("service/importComponent", "")
        val module = virtualFile.getModule(project)
        TestCase.assertNotNull(module)
        val beans = SpringSearchService.getInstance(project).getBeanPsiClassesAnnotatedByComponent(module!!)
        val beanNames = beans.filter { it.psiClass.qualifiedName?.startsWith("com.") == true }
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        TestCase.assertTrue(beanNames.contains("com.app.Application"))
        TestCase.assertTrue(beanNames.contains("com.outer.OuterImport"))
    }

    fun testImportWithBean() {
        val virtualFile = myFixture.copyDirectoryToProject("service/importComponentWithBean", "")
        val module = virtualFile.getModule(project)
        TestCase.assertNotNull(module)
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module!!)
        val beanNames = beans.filter { it.psiClass.qualifiedName?.startsWith("com.") == true }
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        TestCase.assertTrue(beanNames.contains("com.app.Application"))
        TestCase.assertTrue(beanNames.contains("com.outer.OuterImport"))
        TestCase.assertTrue(beanNames.contains("com.outer.OuterBean"))
    }

    fun testImportComplexWithComponentScan() {
        val virtualFile = myFixture.copyDirectoryToProject("service/importComplexWithComponentScan", "")
        val module = virtualFile.getModule(project)
        TestCase.assertNotNull(module)
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module!!)
        val beanNames = beans.filter { it.psiClass.qualifiedName?.startsWith("com.") == true }
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        TestCase.assertTrue(beanNames.contains("com.app.Application"))
        TestCase.assertTrue(beanNames.contains("com.app.AppBean"))
        TestCase.assertTrue(beanNames.contains("com.outer.OuterComponent"))
        TestCase.assertTrue(beanNames.contains("com.outerimport.OuterImport"))
        TestCase.assertTrue(beanNames.contains("com.outerimport.OuterImportBean"))
        TestCase.assertTrue(beanNames.contains("com.outer2.Outer2"))
        TestCase.assertTrue(beanNames.contains("com.outer3.Outer3"))
    }

    fun testComponentMissingBeanSearch() {
        val virtualFile = myFixture.copyDirectoryToProject("service/conditionalOnMissingBean", "")
        val module = virtualFile.getModule(project)
        assertNotNull(module)
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module!!)
        val beanTestClass = beans.filter { it.name == "testClass" }
        assertEquals(1, beanTestClass.size)
        val psiBean = beanTestClass[0]
        assertTrue(psiBean.psiMember is PsiMethod)
        assertTrue((psiBean.psiMember as PsiMethod).containingClass?.qualifiedName == "com.app.AppConfiguration")
    }
}