package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
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
}