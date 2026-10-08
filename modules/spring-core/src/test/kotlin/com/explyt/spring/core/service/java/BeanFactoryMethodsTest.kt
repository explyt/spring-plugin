/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.beans.BeanModelSource
import com.explyt.spring.core.service.beans.BeanSourcePreference
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.progress.util.ProgressIndicatorBase
import com.intellij.openapi.util.Computable
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class BeanFactoryMethodsTest : ExplytJavaLightTestCase() {
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
            "@Bean public Foo foo() { return new Foo(); }\n@Bean public Foo foo(Dep dep) { return new Foo(); }",
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
        configure("@Bean public Foo first() { return new Foo(); }\n@Bean public Foo second() { return new Foo(); }")
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
            "Consumer.java",
            """
            package com.explyt.demo;
            import org.springframework.stereotype.Component;
            import org.springframework.beans.factory.annotation.Autowired;
            @Component
            public class Consumer {
                @Autowired Foo foo;
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
            "@Bean @Override public Foo foo() { return new Foo(); }",
            "class GrandBase { @Bean public Foo foo() { return new Foo(); } } class Base extends GrandBase {}",
            "extends Base"
        )
        assertEquals(psiClass("GrandBase"), psiClass("Base").superClass)
        assertEquals(psiClass("Base"), psiClass("Child").superClass)
        assertEquals(0, psiClass("Base").findMethodsByName("foo", false).size)
        assertTrue(psiClass("GrandBase").findMethodsByName("foo", false).single().hasAnnotation(SpringCoreClasses.BEAN))
        assertTrue(psiClass("Child").findMethodsByName("foo", false).single().hasAnnotation(SpringCoreClasses.BEAN))
        assertFooDeclaredIn("Child")
    }

    fun testInterfaceDefaultOverrideIsOneBeanDeclaredInChild() {
        configure(
            "@Bean @Override public Foo foo() { return new Foo(); }",
            "interface Base { @Bean default Foo foo() { return new Foo(); } }",
            "implements Base"
        )
        assertDefaultFactoryInterface()
        val child = psiClass("Child").findMethodsByName("foo", false).single()
        assertTrue(child.hasAnnotation(SpringCoreClasses.BEAN))
        assertTrue(child.findSuperMethods().any { it.containingClass == psiClass("Base") })
        assertFooDeclaredIn("Child")
    }

    fun testInheritedInterfaceDefaultIsOneBeanDeclaredInInterface() {
        configure("", "interface Base { @Bean default Foo foo() { return new Foo(); } }", "implements Base")
        assertDefaultFactoryInterface()
        assertEquals(0, psiClass("Child").findMethodsByName("foo", false).size)
        assertFooDeclaredIn("Base")
    }

    fun testProjectBeansContainsOneAnnotatedOverride() {
        configureOverride(annotated = true)
        val beans = SpringSearchService.getInstance(project).getProjectBeans(module)
            .filter { it.psiClass.qualifiedName == "com.explyt.demo.Foo" }
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(psiClass("Child"), (beans.single().psiMember as PsiMethod).containingClass)
    }

    fun testCyclicConfigurationQueryTerminatesDeterministically() {
        configure("", "")
        myFixture.addFileToProject(
            "com/explyt/demo/A.java", """
            package com.explyt.demo;
            import org.springframework.context.annotation.*;
            @Configuration(proxyBeanMethods = false)
            public class A extends B { @Bean public Foo foo() { return new Foo(); } }
        """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/explyt/demo/B.java", """
            package com.explyt.demo;
            import org.springframework.context.annotation.*;
            @Configuration(proxyBeanMethods = false)
            public class B extends A {}
        """.trimIndent()
        )
        val a = psiClass("A")
        val b = psiClass("B")
        assertEquals(b, a.superClass)
        assertEquals(a, b.superClass)
        assertTrue(a.hasAnnotation(SpringCoreClasses.CONFIGURATION))
        assertTrue(b.hasAnnotation(SpringCoreClasses.CONFIGURATION))
        assertTrue(a.findMethodsByName("foo", false).single().hasAnnotation(SpringCoreClasses.BEAN))
        val indicator = ProgressIndicatorBase()
        val finished = CountDownLatch(1)
        val result = ApplicationManager.getApplication().executeOnPooledThread(Callable {
            try {
                ProgressManager.getInstance().runProcess(Computable {
                    ApplicationManager.getApplication().runReadAction(Computable {
                        activeFooBeans().map { it.name to (it.psiMember as PsiMethod).containingClass?.qualifiedName }
                    })
                }, indicator)
            } finally {
                finished.countDown()
            }
        })
        try {
            assertEquals(listOf("foo" to "com.explyt.demo.A"), result.get(30, TimeUnit.SECONDS))
        } finally {
            indicator.cancel()
            result.cancel(true)
            assertTrue("Query worker must stop before fixture teardown", finished.await(5, TimeUnit.SECONDS))
        }
    }

    fun testRenamedOverridePreservesSuperclassBeanName() {
        configure(
            "@Bean(\"bar\") @Override public Foo foo() { return new Foo(); }",
            "class Base { @Bean public Foo foo() { return new Foo(); } }",
            "extends Base"
        )
        val base = psiClass("Base").findMethodsByName("foo", false).single()
        val child = psiClass("Child").findMethodsByName("foo", false).single()
        assertResolvedBeanName(base, null)
        assertResolvedBeanName(child, "bar")
        assertTrue("Precondition: Child overrides Base.foo", child.findSuperMethods().contains(base))
        val beans = activeFooBeans()
        assertEquals("Distinct bean names on an override must both survive", setOf("foo", "bar"), beans.map { it.name }.toSet())
        assertEquals(2, beans.size)
        assertEquals(psiClass("Base"), (beans.single { it.name == "foo" }.psiMember as PsiMethod).containingClass)
        assertEquals(psiClass("Child"), (beans.single { it.name == "bar" }.psiMember as PsiMethod).containingClass)
    }

    fun testDifferentlyNamedOverloadsAreTwoActiveBeans() {
        configure(
            "@Bean(\"a\") public Foo foo() { return new Foo(); }\n@Bean(\"b\") public Foo foo(Dep dep) { return new Foo(); }",
            enforceUniqueMethods = false
        )
        val methods = psiClass("Child").findMethodsByName("foo", false)
        assertEquals("Precondition: two overloads", 2, methods.size)
        assertEquals(setOf(0, 1), methods.map { it.parameterList.parametersCount }.toSet())
        assertResolvedBeanName(methods.single { it.parameterList.parametersCount == 0 }, "a")
        assertResolvedBeanName(methods.single { it.parameterList.parametersCount == 1 }, "b")
        val configuration = psiClass("Child").getAnnotation(SpringCoreClasses.CONFIGURATION)!!
        assertEquals(SpringCoreClasses.CONFIGURATION, configuration.resolveAnnotationType()?.qualifiedName)
        assertEquals(false, JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(configuration.findAttributeValue("enforceUniqueMethods")!!))
        val beans = activeFooBeans()
        assertEquals("Distinct bean names on overloads must both survive", setOf("a", "b"), beans.map { it.name }.toSet())
        assertEquals(2, beans.size)
    }

    fun testAbstractInterfaceFactoryDoesNotPublishUnannotatedImplementation() {
        configure(
            "@Override public Foo foo() { return new Foo(); }",
            "interface Factory { @Bean Foo foo(); }",
            "implements Factory"
        )
        val factory = psiClass("Factory")
        assertTrue("Precondition: Factory is an interface", factory.isInterface)
        assertEquals(listOf(factory), psiClass("Child").interfaces.toList())
        val abstractMethod = factory.findMethodsByName("foo", false).single()
        assertResolvedBeanName(abstractMethod, null)
        assertTrue("Precondition: interface factory is abstract", abstractMethod.hasModifierProperty(PsiModifier.ABSTRACT))
        val implementation = psiClass("Child").findMethodsByName("foo", false).single()
        assertFalse("Precondition: implementation has no @Bean", implementation.hasAnnotation(SpringCoreClasses.BEAN))
        assertTrue("Precondition: implementation overrides interface method", implementation.findSuperMethods().contains(abstractMethod))
        assertEquals("Abstract interface @Bean must not publish a foo bean", emptyList<String>(), activeFooBeans().map { it.name })
    }

    private fun assertResolvedBeanName(method: PsiMethod, explicitName: String?) {
        val annotation = method.getAnnotation(SpringCoreClasses.BEAN) ?: error("Missing @Bean on ${method.name}")
        assertEquals("Precondition: @Bean annotation resolves", SpringCoreClasses.BEAN, annotation.resolveAnnotationType()?.qualifiedName)
        val value = annotation.findDeclaredAttributeValue("value")
        if (explicitName == null) {
            assertNull("Precondition: default bean name", value)
        } else {
            assertEquals("Precondition: explicit value is resolved", explicitName, JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(value!!))
        }
    }

    private fun assertDefaultFactoryInterface() {
        val base = psiClass("Base")
        assertTrue(base.isInterface)
        assertEquals(listOf(base), psiClass("Child").interfaces.toList())
        val method = base.findMethodsByName("foo", false).single()
        assertTrue(method.hasModifierProperty("default"))
        assertTrue(method.hasAnnotation(SpringCoreClasses.BEAN))
    }

    private fun assertFooDeclaredIn(owner: String) {
        val beans = activeFooBeans()
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(psiClass(owner), (beans.single().psiMember as PsiMethod).containingClass)
    }

    private fun configureOverride(annotated: Boolean) {
        configure(
            "${if (annotated) "@Bean " else ""}@Override public Foo foo() { return new Foo(); }",
            "class Base { @Bean public Foo foo() { return new Foo(); } }",
            "extends Base"
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
        enforceUniqueMethods: Boolean = true
    ) {
        myFixture.addFileToProject(
            "com/explyt/demo/App.java",
            "package com.explyt.demo; @org.springframework.boot.autoconfigure.SpringBootApplication public class App {}"
        )
        myFixture.addFileToProject(
            "com/explyt/demo/Child.java",
            """
            package com.explyt.demo;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            class Foo {}
            class Dep {}
            $base
            @Configuration(proxyBeanMethods = false, enforceUniqueMethods = $enforceUniqueMethods)
            public class Child $inheritance {
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
