/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.util.ExplytAnnotationUtil.getStringMemberValues
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchUtils
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiManager
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import junit.framework.TestCase

class SpringSearchServiceTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

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
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project)
        TestCase.assertNotNull(module)
        val beans = SpringSearchService.getInstance(project).getBeanPsiClassesAnnotatedByComponent(module!!)
        val beanNames = beans.mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        TestCase.assertTrue(beanNames.contains("com.app.Application"))
        TestCase.assertTrue(beanNames.contains("com.outer.OuterImport"))
    }

    fun testImportWithBean() {
        val virtualFile = myFixture.copyDirectoryToProject("service/importComponentWithBean", "")
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project)
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
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project)
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
        val module = ModuleUtilCore.findModuleForFile(virtualFile, project)
        assertNotNull(module)
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module!!)
        val beanTestClass = beans.filter { it.name == "testClass" }
        assertEquals(1, beanTestClass.size)
        val psiBean = beanTestClass[0]
        assertTrue(psiBean.psiMember is PsiMethod)
        assertTrue((psiBean.psiMember as PsiMethod).containingClass?.qualifiedName == "com.app.AppConfiguration")
    }

    fun testAllReferencesToElementReflectSourceChanges() {
        val file = myFixture.configureByText(
            "References.java",
            """
            class References {
                void setPrefix(String value) {}
                void first() { setPrefix(\"first\"); }
            }
            """.trimIndent()
        )
        val method = PsiTreeUtil.findChildrenOfType(file, PsiMethod::class.java)
            .single { it.name == "setPrefix" }

        assertEquals(1, SpringSearchUtils.getAllReferencesToElement(method).size)

        val document = file.viewProvider.document!!
        WriteCommandAction.runWriteCommandAction(project) {
            val insertionOffset = document.text.lastIndexOf('}')
            document.insertString(insertionOffset, "    void second() { setPrefix(\\\"second\\\"); }\\n")
        }
        PsiDocumentManager.getInstance(project).commitDocument(document)
        PsiManager.getInstance(project).dropPsiCaches()

        assertEquals(2, SpringSearchUtils.getAllReferencesToElement(method).size)
    }

    fun testBeanNameAttributeInActiveModel() = assertBeanName("@Bean(name = \"x\")", "name", listOf("x"), "x")

    fun testBeanNamePositionalControl() = assertBeanName("@Bean(\"x\")", "value", listOf("x"), "x")

    fun testBeanNameValueControl() = assertBeanName("@Bean(value = \"x\")", "value", listOf("x"), "x")

    fun testBeanNameUnnamedControl() = assertBeanName("@Bean", "value", emptyList(), "foo")

    fun testBeanNameAttributeAndValueWithSameName() = assertBeanName("@Bean(value = \"x\", name = \"x\")", "value", listOf("x"), "x")

    fun testBlankBeanNameFallsBackToMethodName() = assertBeanName("@Bean(name = \"\")", "name", listOf(""), "foo")

    fun testBeanNameAttributeSnapshotAliases() {
        val application = configureBeanName("@Bean(name = {\"x\", \"y\"})", "name", listOf("x", "y"))
        val records = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.STATIC).records
            .filter { it.typeName == "beanname.Foo" }
        assertEquals("One factory must produce one snapshot record", 1, records.size)
        assertEquals("x", records.single().name)
        assertEquals(listOf("x", "y"), records.single().knownNames.toList())
    }

    private fun assertBeanName(annotation: String, attribute: String, values: List<String>, expectedName: String) {
        val application = configureBeanName(annotation, attribute, values)
        val records = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(application, BeanSourcePreference.STATIC).records
            .filter { it.typeName == "beanname.Foo" }
        assertEquals(1, records.size)
        assertEquals(expectedName, records.single().name)
        assertEquals(listOf(expectedName), records.single().knownNames.toList())
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiClass.qualifiedName == "beanname.Foo" }
        assertEquals(setOf(expectedName), beans.map { it.name }.toSet())
        assertEquals(1, beans.size)
    }

    private fun configureBeanName(annotation: String, attribute: String, values: List<String>): PsiClass {
        myFixture.addFileToProject(
            "beanname/App.java",
            """
            package beanname;
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            import org.springframework.context.annotation.Bean;
            @SpringBootApplication
            public class App {
                $annotation
                public Foo foo() { return new Foo(); }
            }
            class Foo {}
            """.trimIndent()
        )
        val application = JavaPsiFacade.getInstance(project)
            .findClass("beanname.App", GlobalSearchScope.projectScope(project))!!
        val beanAnnotation = application.findMethodsByName("foo", false).single()
            .getAnnotation(SpringCoreClasses.BEAN)!!
        assertEquals(SpringCoreClasses.BEAN, beanAnnotation.resolveAnnotationType()?.qualifiedName)
        assertTrue(values.none { it == "foo" })
        assertEquals("Declared @Bean attribute values", values, beanAnnotation.getStringMemberValues(attribute))
        assertEquals("Explicit attribute presence", values.isNotEmpty(), beanAnnotation.findDeclaredAttributeValue(attribute) != null)
        return application
    }
}
