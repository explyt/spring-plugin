/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class ComponentCandidateTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false)
    }

    override fun tearDown() {
        super.tearDown()
        Registry.get("explyt.spring.root.runConfiguration").resetToDefault()
    }

    fun testAbstractComponentIsNotBean() {
        configure(
            "AbstractComponent", """
            @Component
            abstract class AbstractComponent
            """
        )
        assertNotBean("candidates.AbstractComponent")
    }

    fun testAbstractComponentWithLookupMethodIsBean() {
        configure(
            "LookupComponent", """
            import org.springframework.beans.factory.annotation.Lookup
            @Component
            abstract class LookupComponent {
                @Lookup
                abstract fun concrete(): Concrete
            }
            """
        )
        assertSingleBean("candidates.LookupComponent")
    }

    fun testComponentInterfaceIsNotBean() {
        configure(
            "ComponentInterface", """
            @Component
            interface ComponentInterface
            """
        )
        assertNotBean("candidates.ComponentInterface")
    }

    fun testInnerComponentIsNotBean() {
        configure(
            "Outer", """
            class Outer {
                @Component
                inner class Inner
            }
            """
        )
        assertNotBean("candidates.Outer.Inner")
    }

    fun testNestedComponentIsBean() {
        configure(
            "Outer", """
            class Outer {
                @Component
                class Nested
            }
            """
        )
        assertSingleBean("candidates.Outer.Nested")
    }

    fun testAbstractConfigurationIsNotBean() {
        configureAbstractConfiguration()
        assertNotBean("candidates.BaseConfig")
    }

    fun testBeanMethodOfAbstractConfigurationStaysBeanThroughSubclass() {
        configureAbstractConfiguration()
        assertSingleBean("candidates.AppConfig")
        val fooBeans = activeBeans().filter { it.psiClass.qualifiedName == "candidates.Foo" }
        assertEquals(listOf("foo"), fooBeans.map { it.name })
    }

    private fun configureAbstractConfiguration() {
        configure(
            "BaseConfig", """
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            @Configuration
            abstract class BaseConfig {
                @Bean
                fun foo(): Foo = Foo()
            }
            """
        )
        addClass(
            "AppConfig", """
            import org.springframework.context.annotation.Configuration
            @Configuration
            class AppConfig : BaseConfig()
            """
        )
        addClass("Foo", "class Foo")
    }

    private fun configure(className: String, body: String) {
        addClass(
            "App", """
            import org.springframework.boot.autoconfigure.SpringBootApplication
            @SpringBootApplication
            class App
            """
        )
        addClass(
            "Concrete", """
            @Component
            class Concrete
            """
        )
        addClass(className, body)
    }

    private fun addClass(className: String, body: String) {
        myFixture.addFileToProject(
            "candidates/$className.kt",
            "package candidates\nimport org.springframework.stereotype.Component\n" + body.trimIndent()
        )
    }

    private fun assertNotBean(qualifiedName: String) {
        assertReachedByComponentScan(qualifiedName)
        val beans = activeBeans().filter { it.psiClass.qualifiedName == qualifiedName }
        assertEquals("$qualifiedName must not be a bean", emptyList<String>(), beans.map { it.name })
    }

    private fun assertSingleBean(qualifiedName: String) {
        assertReachedByComponentScan(qualifiedName)
        val beans = activeBeans().filter { it.psiMember == it.psiClass && it.psiClass.qualifiedName == qualifiedName }
        assertEquals("$qualifiedName must be one bean", 1, beans.size)
    }

    private fun assertReachedByComponentScan(qualifiedName: String) {
        val psiClass = findClass(qualifiedName)
        assertTrue("$qualifiedName must carry @Component", psiClass.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        val packages = PackageScanService.getInstance(project).getAllPackages().getPackages(module)
        assertTrue("$qualifiedName must be in a scanned package $packages", packages.any { qualifiedName.startsWith(it) })
        val concrete = activeBeans().filter { it.psiClass.qualifiedName == "candidates.Concrete" }
        assertEquals("A concrete sibling must be found by the same scan", 1, concrete.size)
    }

    private fun findClass(qualifiedName: String): PsiClass {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))
        assertNotNull("$qualifiedName must exist", psiClass)
        return psiClass!!
    }

    private fun activeBeans(): Set<PsiBean> = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
}
