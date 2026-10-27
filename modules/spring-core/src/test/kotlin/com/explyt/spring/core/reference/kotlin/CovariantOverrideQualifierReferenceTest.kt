/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.core.references.CompleteBeanReference
import com.explyt.spring.core.service.PsiBean
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil

class CovariantOverrideQualifierReferenceTest : ExplytKotlinLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false, testRootDisposable)
    }

    fun testCompletionOffersCovariantOverrideBeanByOverrideType() {
        configureCovariantOverride()
        configureConsumer("Dog", "<caret>")
        assertEquals(setOf("animal"), qualifierReference().variantNames())
    }

    fun testQualifierResolvesCovariantOverrideBeanByOverrideType() {
        val bean = configureCovariantOverride()
        configureConsumer("Dog", "anim<caret>al")
        assertResolvesTo(bean.psiMember, qualifierReference().resolve())
    }

    fun testCompletionOffersCovariantOverrideBeanBySuperclassType() {
        configureCovariantOverride()
        configureConsumer("Animal", "<caret>")
        assertEquals(setOf("animal"), qualifierReference().variantNames())
    }

    fun testQualifierResolvesCovariantOverrideBeanBySuperclassType() {
        val bean = configureCovariantOverride()
        configureConsumer("Animal", "anim<caret>al")
        assertResolvesTo(bean.psiMember, qualifierReference().resolve())
    }

    fun testCompletionOffersBeanDeclaredWithNarrowType() {
        configureNarrowFactory()
        configureConsumer("Dog", "<caret>")
        assertEquals(setOf("animal"), qualifierReference().variantNames())
    }

    fun testQualifierResolvesBeanDeclaredWithNarrowType() {
        val bean = configureNarrowFactory()
        configureConsumer("Dog", "anim<caret>al")
        assertResolvesTo(bean.psiMember, qualifierReference().resolve())
    }

    private fun configureCovariantOverride(): PsiBean {
        configureConfiguration(
            base = "open class Base { @Bean open fun animal(): Animal = Animal() }",
            body = "override fun animal(): Dog = Dog()"
        )
        val base = animalMethodOf("Base")
        val child = animalMethodOf("Child")
        assertEquals("com.explyt.demo.Animal", base.returnType?.canonicalText)
        assertEquals("Precondition: override narrows the return type", "com.explyt.demo.Dog", child.returnType?.canonicalText)
        assertTrue("Precondition: Child overrides Base.animal", child.findSuperMethods().contains(base))
        return activeAnimalBean()
    }

    private fun configureNarrowFactory(): PsiBean {
        configureConfiguration(
            base = "open class Base { @Bean open fun animal(): Dog = Dog() }",
            body = ""
        )
        assertEquals("Precondition: Child declares no override", 0, psiClass("Child").findMethodsByName("animal", false).size)
        assertEquals("com.explyt.demo.Dog", animalMethodOf("Base").returnType?.canonicalText)
        return activeAnimalBean()
    }

    private fun configureConfiguration(base: String, body: String) {
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
            open class Animal
            class Dog : Animal()
            $base
            @Configuration(proxyBeanMethods = false)
            open class Child : Base() {
                $body
            }
            """.trimIndent()
        )
    }

    private fun configureConsumer(type: String, qualifier: String) {
        myFixture.configureByText(
            "Consumer.java",
            """
            package com.explyt.demo;
            import org.springframework.stereotype.Component;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            @Component
            public class Consumer {
                @Autowired @Qualifier("$qualifier") $type injected;
            }
            """.trimIndent()
        )
        assertEquals("com.explyt.demo.$type", psiClass("Consumer").findFieldByName("injected", false)?.type?.canonicalText)
    }

    private fun activeAnimalBean(): PsiBean {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module).filter { it.name == "animal" }
        assertEquals("Precondition: the bean model holds one animal bean", 1, beans.size)
        assertEquals(
            "Precondition: the bean model types animal as Dog",
            "com.explyt.demo.Dog",
            beans.single().psiClass.qualifiedName
        )
        return beans.single()
    }

    private fun qualifierReference(): CompleteBeanReference {
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)
        val host = PsiTreeUtil.getParentOfType(leaf, PsiLanguageInjectionHost::class.java, false)
            ?: error("No string literal at the caret")
        return host.references.filterIsInstance<CompleteBeanReference>().singleOrNull()
            ?: error("No qualifier bean reference on ${host.text}: ${host.references.toList()}")
    }

    private fun CompleteBeanReference.variantNames(): Set<String> =
        variants.map { (it as LookupElement).lookupString }.toSet()

    private fun assertResolvesTo(expected: PsiElement, actual: PsiElement?) {
        assertTrue(
            "Qualifier must resolve to ${describe(expected)}, got ${describe(actual)}",
            actual != null && psiManager.areElementsEquivalent(expected, actual)
        )
    }

    private fun describe(element: PsiElement?): String =
        (element as? PsiMethod)?.let { "${it.containingClass?.name}.${it.name}" } ?: element.toString()

    private fun animalMethodOf(className: String): PsiMethod =
        psiClass(className).findMethodsByName("animal", false).single()

    private fun psiClass(name: String) = JavaPsiFacade.getInstance(project)
        .findClass("com.explyt.demo.$name", GlobalSearchScope.projectScope(project)) ?: error("No PSI for $name")
}
