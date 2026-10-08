/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.beans.BeanModelSource
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

class BeanFactoryMethodsTest : ExplytKotlinLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false, testRootDisposable)
    }

    fun testAnnotatedOverrideIsOneActiveBeanDeclaredInChild() {
        configureOverride(annotated = true)
        val beans = activeFooBeans()
        assertEquals("Annotated override must register one foo bean", 1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals("com.explyt.demo.Child", (beans.single().psiMember as PsiMethod).containingClass?.qualifiedName)
    }

    fun testUnannotatedOverrideKeepsSuperclassBeanMetadata() {
        configureOverride(annotated = false)
        val beans = activeFooBeans()
        assertEquals("Unannotated override must keep one superclass foo bean", 1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals("com.explyt.demo.Base", (beans.single().psiMember as PsiMethod).containingClass?.qualifiedName)
    }

    fun testSameNameOverloadsAreOneActiveBean() {
        configure(
            "@Bean fun foo(): Foo = Foo()\n@Bean fun foo(dep: Dep): Foo = Foo()",
            enforceUniqueMethods = false
        )
        val methods = psiClass("Child").findMethodsByName("foo", false)
        assertEquals("Precondition: two local overloads", 2, methods.size)
        assertEquals(setOf(0, 1), methods.map { it.parameterList.parametersCount }.toSet())
        assertTrue("Precondition: both overloads are @Bean", methods.all { it.hasAnnotation(SpringCoreClasses.BEAN) })
        val beans = activeFooBeans()
        assertEquals("Same-name overloads must register one foo bean", 1, beans.size)
        assertEquals("foo", beans.single().name)
    }

    fun testDifferentMethodNamesRemainTwoActiveBeans() {
        configure("@Bean fun first(): Foo = Foo()\n@Bean fun second(): Foo = Foo()")
        val methods = psiClass("Child").methods.filter { it.hasAnnotation(SpringCoreClasses.BEAN) }
        assertEquals(
            "Precondition: two distinct @Bean declarations",
            setOf("first", "second"),
            methods.map { it.name }.toSet()
        )
        val beans = activeFooBeans()
        assertEquals("Distinct factory names must remain two beans", 2, beans.size)
        assertEquals(setOf("first", "second"), beans.map { it.name }.toSet())
    }

    fun testAnnotatedOverrideDoesNotMakeAutowiringAmbiguous() {
        configureOverride(annotated = true)
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
        myFixture.configureByText(
            "Consumer.kt",
            """
            package com.explyt.demo
            import org.springframework.stereotype.Component
            import org.springframework.beans.factory.annotation.Autowired
            @Component
            class Consumer {
                @Autowired lateinit var foo: Foo
            }
            """.trimIndent()
        )
        val injection = psiClass("Consumer").findFieldByName("foo", false)!!
        assertTrue(
            "Precondition: Foo injection is @Autowired",
            injection.hasAnnotation("org.springframework.beans.factory.annotation.Autowired")
        )
        assertEquals("com.explyt.demo.Foo", injection.type.canonicalText)
        myFixture.testHighlighting(true, false, false)
    }

    fun testStaticSnapshotHasOneRecordForAnnotatedOverride() {
        configureOverride(annotated = true)
        val snapshot = SpringSearchServiceFacade.getInstance(project)
            .getBeanSnapshot(psiClass("App"), BeanSourcePreference.STATIC)
        assertEquals(BeanModelSource.STATIC, snapshot.selection.source)
        val records = snapshot.records.filter { it.typeName == "com.explyt.demo.Foo" }
        assertEquals("Static snapshot must contain one foo record", 1, records.size)
        assertEquals("foo", records.single().name)
        assertEquals(
            "com.explyt.demo.Child",
            (records.single().declaration as PsiMethod).containingClass?.qualifiedName
        )
    }

    fun testGrandparentOverrideIsOneBeanDeclaredInChild() {
        configure(
            "@Bean override fun foo(): Foo = Foo()",
            "open class GrandBase { @Bean open fun foo(): Foo = Foo() } open class Base : GrandBase()",
            ": Base()"
        )
        assertEquals(psiClass("GrandBase"), psiClass("Base").superClass)
        assertEquals(psiClass("Base"), psiClass("Child").superClass)
        assertEquals(0, psiClass("Base").findMethodsByName("foo", false).size)
        assertTrue(psiClass("GrandBase").findMethodsByName("foo", false).single().hasAnnotation(SpringCoreClasses.BEAN))
        assertTrue(psiClass("Child").findMethodsByName("foo", false).single().hasAnnotation(SpringCoreClasses.BEAN))
        val beans = activeFooBeans()
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(psiClass("Child"), (beans.single().psiMember as PsiMethod).containingClass)
    }

    fun testProjectBeansContainsOneOpenClassOverride() {
        configureOverride(annotated = true)
        val configuration = psiClass("Child").getAnnotation(SpringCoreClasses.CONFIGURATION)!!
        assertEquals("false", configuration.findAttributeValue("proxyBeanMethods")?.text)
        val beans = SpringSearchService.getInstance(project).getProjectBeans(module)
            .filter { it.psiClass.qualifiedName == "com.explyt.demo.Foo" }
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(psiClass("Child"), (beans.single().psiMember as PsiMethod).containingClass)
    }

    fun testObjectConfigurationPublishesItsFactory() {
        configure("@Bean fun foo(): Foo = Foo()", declaration = "object")
        assertTrue(
            "Precondition: object light class has INSTANCE",
            psiClass("Child").findFieldByName("INSTANCE", false) != null
        )
        assertTrue(psiClass("Child").findMethodsByName("foo", false).single().hasAnnotation(SpringCoreClasses.BEAN))
        val beans = activeFooBeans()
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(psiClass("Child"), (beans.single().psiMember as PsiMethod).containingClass)
    }

    private fun configureOverride(annotated: Boolean) {
        configure(
            "${if (annotated) "@Bean " else ""}override fun foo(): Foo = Foo()",
            "open class Base { @Bean open fun foo(): Foo = Foo() }",
            ": Base()"
        )
        val base = psiClass("Base").findMethodsByName("foo", false).single()
        val child = psiClass("Child").findMethodsByName("foo", false).single()
        assertTrue("Precondition: superclass factory is @Bean", base.hasAnnotation(SpringCoreClasses.BEAN))
        assertEquals("Precondition: override annotation", annotated, child.hasAnnotation(SpringCoreClasses.BEAN))
        assertTrue("Precondition: Child overrides Base.foo", child.findSuperMethods().any { it == base })
    }

    private fun configure(
        body: String,
        base: String = "",
        inheritance: String = "",
        enforceUniqueMethods: Boolean = true,
        declaration: String = "open class"
    ) {
        myFixture.addFileToProject(
            "com/explyt/demo/App.kt",
            "package com.explyt.demo\n@org.springframework.boot.autoconfigure.SpringBootApplication(proxyBeanMethods = false) class App"
        )
        myFixture.addFileToProject(
            "com/explyt/demo/Child.kt",
            """
            package com.explyt.demo
            import org.springframework.context.annotation.Bean
            import org.springframework.context.annotation.Configuration
            class Foo
            class Dep
            $base
            @Configuration(proxyBeanMethods = false, enforceUniqueMethods = $enforceUniqueMethods)
            $declaration Child $inheritance {
                $body
            }
            """.trimIndent()
        )
        assertTrue(
            "Precondition: Child is @Configuration",
            psiClass("Child").hasAnnotation(SpringCoreClasses.CONFIGURATION)
        )
    }

    private fun psiClass(name: String) = JavaPsiFacade.getInstance(project)
        .findClass("com.explyt.demo.$name", GlobalSearchScope.projectScope(project)) ?: error("No PSI for $name")

    private fun activeFooBeans() = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        .filter { it.psiClass.qualifiedName == "com.explyt.demo.Foo" }
}
