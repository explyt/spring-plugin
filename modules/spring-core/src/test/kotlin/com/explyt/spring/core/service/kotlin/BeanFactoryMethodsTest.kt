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
import com.explyt.spring.core.util.BeanFactoryMethods
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.PsiModifier
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

    fun testSameBeanNameDifferentMethodsInOneClassAreBothListed() {
        configure("@Bean fun admin(): Foo = Foo()\n@Bean(\"admin\") fun adminFactory(): Foo = Foo()")
        val configuration = psiClass("Child")
        val methods = listOf("admin", "adminFactory").map { configuration.findMethodsByName(it, false).single() }
        assertTrue("Precondition: both local methods are @Bean", methods.all { it.hasAnnotation(SpringCoreClasses.BEAN) })
        assertResolvedBeanName(methods[0], null)
        assertResolvedBeanName(methods[1], "admin")
        val factories = BeanFactoryMethods.of(configuration).toList()
        assertEquals("Same bean name with different method names must keep both factories", 2, factories.size)
        assertEquals(listOf("admin", "adminFactory"), factories.map { it.name })
        val beans = activeFooBeans()
        assertEquals(listOf("admin", "admin"), beans.map { it.name })
        assertEquals(setOf("admin", "adminFactory"), beans.map { (it.psiMember as PsiMethod).name }.toSet())
    }

    fun testSameBeanNameSameMethodNameOverloadsAreMerged() {
        configure(
            "@Bean(\"a\") fun foo(): Foo = Foo()\n@Bean(\"a\") fun foo(dep: Dep): Foo = Foo()",
            enforceUniqueMethods = false
        )
        val configuration = psiClass("Child")
        val methods = configuration.findMethodsByName("foo", false)
        assertEquals("Precondition: two local overloads", 2, methods.size)
        assertEquals(setOf(0, 1), methods.map { it.parameterList.parametersCount }.toSet())
        methods.forEach { assertResolvedBeanName(it, "a") }
        val annotation = configuration.getAnnotation(SpringCoreClasses.CONFIGURATION)!!
        assertEquals(false, JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(annotation.findAttributeValue("enforceUniqueMethods")!!))
        assertEquals(listOf("foo"), BeanFactoryMethods.of(configuration).map { it.name }.toList())
        val beans = activeFooBeans()
        assertEquals(listOf("a"), beans.map { it.name })
        assertEquals("foo", (beans.single().psiMember as PsiMethod).name)
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

    fun testRenamedOverridePreservesSuperclassBeanName() {
        configure(
            "@Bean(\"bar\") override fun foo(): Foo = Foo()",
            "open class Base { @Bean open fun foo(): Foo = Foo() }",
            ": Base()"
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

    fun testNameAttributeOverridePreservesSuperclassBeanName() {
        configure(
            "@Bean(name = [\"bar\"]) override fun foo(): Foo = Foo()",
            "open class Base { @Bean open fun foo(): Foo = Foo() }",
            ": Base()"
        )
        val base = psiClass("Base").findMethodsByName("foo", false).single()
        val child = psiClass("Child").findMethodsByName("foo", false).single()
        assertResolvedBeanName(base, null)
        assertExplicitNameWithoutValue(child, "bar")
        assertTrue("Precondition: Child overrides Base.foo", child.findSuperMethods().contains(base))
        val beans = activeFooBeans()
        assertEquals(setOf("foo", "bar"), beans.map { it.name }.toSet())
        assertEquals(2, beans.size)
        assertEquals(psiClass("Base"), (beans.single { it.name == "foo" }.psiMember as PsiMethod).containingClass)
        assertEquals(psiClass("Child"), (beans.single { it.name == "bar" }.psiMember as PsiMethod).containingClass)
    }

    fun testSameNameAttributeOverrideIsOneBeanDeclaredInChild() {
        configure(
            "@Bean(name = [\"x\"]) override fun foo(): Foo = Foo()",
            "open class Base { @Bean(name = [\"x\"]) open fun foo(): Foo = Foo() }",
            ": Base()"
        )
        val base = psiClass("Base").findMethodsByName("foo", false).single()
        val child = psiClass("Child").findMethodsByName("foo", false).single()
        assertExplicitNameWithoutValue(base, "x")
        assertExplicitNameWithoutValue(child, "x")
        assertTrue("Precondition: Child overrides Base.foo", child.findSuperMethods().contains(base))
        val beans = activeFooBeans()
        assertEquals(1, beans.size)
        assertEquals("x", beans.single().name)
        assertEquals(psiClass("Child"), (beans.single().psiMember as PsiMethod).containingClass)
    }

    private fun assertExplicitNameWithoutValue(method: PsiMethod, name: String) {
        val annotation = method.getAnnotation(SpringCoreClasses.BEAN) ?: error("Missing @Bean on ${method.name}")
        assertEquals(SpringCoreClasses.BEAN, annotation.resolveAnnotationType()?.qualifiedName)
        assertNull("Precondition: no explicit value attribute", annotation.findDeclaredAttributeValue("value"))
        val explicitName = annotation.findDeclaredAttributeValue("name") as? com.intellij.psi.PsiArrayInitializerMemberValue
            ?: error("Missing explicit name array")
        assertEquals(name, JavaPsiFacade.getInstance(project).constantEvaluationHelper.computeConstantExpression(explicitName.initializers.single()))
    }

    fun testDifferentlyNamedOverloadsAreTwoActiveBeans() {
        configure(
            "@Bean(\"a\") fun foo(): Foo = Foo()\n@Bean(\"b\") fun foo(dep: Dep): Foo = Foo()",
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
            "override fun foo(): Foo = Foo()",
            "interface Factory { @Bean fun foo(): Foo }",
            ": Factory"
        )
        val factory = psiClass("Factory")
        assertTrue("Precondition: Factory is an interface", factory.isInterface)
        assertEquals(listOf(factory), psiClass("Child").interfaces.toList())
        val abstractMethod = factory.findMethodsByName("foo", false).single()
        assertResolvedBeanName(abstractMethod, null)
        assertTrue("Precondition: Kotlin light interface factory is abstract", abstractMethod.hasModifierProperty(PsiModifier.ABSTRACT))
        val implementation = psiClass("Child").findMethodsByName("foo", false).single()
        assertFalse("Precondition: implementation has no @Bean", implementation.hasAnnotation(SpringCoreClasses.BEAN))
        assertTrue("Precondition: implementation overrides interface method", implementation.findSuperMethods().contains(abstractMethod))
        assertEquals("Abstract interface @Bean must not publish a foo bean", emptyList<String>(), activeFooBeans().map { it.name })
    }

    fun testAnnotatedImplementationOfAbstractInterfaceFactoryPublishesOneBean() {
        configure(
            "@Bean override fun foo(): Foo = Foo()",
            "interface Factory { @Bean fun foo(): Foo }",
            ": Factory"
        )
        val factory = psiClass("Factory")
        assertTrue(factory.isInterface)
        assertEquals(listOf(factory), psiClass("Child").interfaces.toList())
        val abstractMethod = factory.findMethodsByName("foo", false).single()
        assertResolvedBeanName(abstractMethod, null)
        assertTrue(abstractMethod.hasModifierProperty(PsiModifier.ABSTRACT))
        val implementation = psiClass("Child").findMethodsByName("foo", false).single()
        assertResolvedBeanName(implementation, null)
        assertTrue(implementation.findSuperMethods().contains(abstractMethod))
        val beans = activeFooBeans()
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(psiClass("Child"), (beans.single().psiMember as PsiMethod).containingClass)
    }

    fun testInheritedInterfaceFactoryWithBodyPublishesOneBean() {
        configure("", "interface Factory { @Bean fun foo(): Foo = Foo() }", ": Factory", declaration = "class")
        val factory = psiClass("Factory")
        assertTrue("Precondition: Factory is an interface", factory.isInterface)
        assertEquals(listOf(factory), psiClass("Child").interfaces.toList())
        assertEquals("Precondition: configuration does not override foo", 0, psiClass("Child").findMethodsByName("foo", false).size)
        val method = factory.findMethodsByName("foo", false).single()
        assertResolvedBeanName(method, null)
        assertFalse("Precondition: light interface method with body is concrete", method.hasModifierProperty(PsiModifier.ABSTRACT))
        assertTrue("Precondition: light interface method with body is default", method.hasModifierProperty(PsiModifier.DEFAULT))
        val beans = activeFooBeans()
        assertEquals(1, beans.size)
        assertEquals("foo", beans.single().name)
        assertEquals(factory, (beans.single().psiMember as PsiMethod).containingClass)
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
