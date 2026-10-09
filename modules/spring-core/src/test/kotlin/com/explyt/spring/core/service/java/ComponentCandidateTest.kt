/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.search.GlobalSearchScope

class ComponentCandidateTest : ExplytJavaLightTestCase() {
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
            public abstract class AbstractComponent {}
            """
        )
        assertNotBean("candidates.AbstractComponent")
    }

    fun testAbstractComponentWithLookupMethodIsBean() {
        configure(
            "LookupComponent", """
            import org.springframework.beans.factory.annotation.Lookup;
            @Component
            public abstract class LookupComponent {
                @Lookup
                public abstract Concrete concrete();
            }
            """
        )
        assertSingleBean("candidates.LookupComponent")
    }

    fun testComponentInterfaceStaysBean() {
        configure(
            "ComponentInterface", """
            @Component
            public interface ComponentInterface {}
            """
        )
        assertSingleBean("candidates.ComponentInterface")
    }

    fun testRepositoryInterfaceWithoutSpringDataStaysBean() {
        configure(
            "Mapper", """
            import org.springframework.stereotype.Repository;
            @Repository
            public interface Mapper {
                String find(long id);
            }
            """
        )
        assertSingleBean("candidates.Mapper")
    }

    fun testRecordComponentIsBean() {
        configure(
            "RecordComponent", """
            @Component
            public record RecordComponent(Concrete concrete) {}
            """
        )
        assertSingleBean("candidates.RecordComponent")
    }

    fun testComponentNestedInInterfaceIsBean() {
        configure(
            "Holder", """
            public interface Holder {
                @Component
                class Nested {}
            }
            """
        )
        assertSingleBean("candidates.Holder.Nested")
    }

    fun testNonStaticInnerComponentIsNotBean() {
        configure(
            "Outer", """
            public class Outer {
                @Component
                public class Inner {}
            }
            """
        )
        assertNotBean("candidates.Outer.Inner")
    }

    fun testStaticNestedComponentIsBean() {
        configure(
            "Outer", """
            public class Outer {
                @Component
                public static class Nested {}
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
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            @Configuration
            public abstract class BaseConfig {
                @Bean
                public Foo foo() { return new Foo(); }
            }
            """
        )
        addClass(
            "AppConfig", """
            import org.springframework.context.annotation.Configuration;
            @Configuration
            public class AppConfig extends BaseConfig {}
            """
        )
        addClass("Foo", "public class Foo {}")
    }

    private fun configure(className: String, body: String) {
        addClass(
            "App", """
            import org.springframework.boot.autoconfigure.SpringBootApplication;
            @SpringBootApplication
            public class App {}
            """
        )
        addClass(
            "Concrete", """
            @Component
            public class Concrete {}
            """
        )
        addClass(className, body)
    }

    private fun addClass(className: String, body: String) {
        myFixture.addFileToProject(
            "candidates/$className.java",
            "package candidates;\nimport org.springframework.stereotype.Component;\n" + body.trimIndent()
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
