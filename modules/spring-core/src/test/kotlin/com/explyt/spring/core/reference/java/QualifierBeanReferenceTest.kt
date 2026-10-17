/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.java

import com.explyt.spring.core.references.CompleteBeanReference
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.ProfileActivation
import com.explyt.spring.core.service.ProfilesService
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.UAnnotation
import org.jetbrains.uast.getParentOfType
import org.jetbrains.uast.toUElement

class QualifierBeanReferenceTest : ExplytJavaLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false, testRootDisposable)
    }

    fun testFieldCompletionOffersOnlyMatchingBeanType() {
        configureBeans()
        configureConsumer("@Autowired @Qualifier(\"<caret>\") Animal injected;")
        assertEquals(setOf("animal"), qualifierReference().variantNames())
    }

    fun testFieldQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("@Autowired @Qualifier(\"anim<caret>al\") Animal injected;")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testConstructorParameterQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("Consumer(@Qualifier(\"anim<caret>al\") Animal injected) {}")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testSetterParameterQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("@Autowired void setAnimal(@Qualifier(\"anim<caret>al\") Animal injected) {}")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testEscapedValueReferenceUsesWholeHostValueRange() {
        val bean = configureBeans("ani\\nmal")
        configureConsumer("@Autowired @Qualifier(\"ani\\nm<caret>al\") Animal injected;")
        val host = qualifierHost()
        val reference = qualifierReference()
        assertEquals(TextRange(1, host.textLength - 1), reference.rangeInElement)
        assertEquals("ani\\nmal", reference.value)
        assertResolvesTo(bean, reference)
    }

    fun testCompletionKeepsProfileInactiveImplementationAndDropsConditionInactiveOne() {
        configurePetImplementations()
        configureConsumer("@Autowired @Qualifier(\"<caret>\") Pet injected;")
        assertEquals(setOf("dog", "cat"), qualifierReference().variantNames())
    }

    fun testCompletionDropsImportedImplementationExcludedByConditionDespiteItsProfile() {
        configurePetImplementations()
        myFixture.addFileToProject(
            "com/explyt/demo/Parrot.java",
            """
            package com.explyt.demo;
            @org.springframework.stereotype.Component
            @org.springframework.context.annotation.Profile("dev")
            @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "feature.bird", havingValue = "true")
            public class Parrot implements Pet {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/explyt/demo/ParrotImporter.java",
            """
            package com.explyt.demo;
            @org.springframework.context.annotation.Configuration
            @org.springframework.context.annotation.Import(Parrot.class)
            public class ParrotImporter {}
            """.trimIndent()
        )
        assertParrotImportedAndExcluded()
        configureConsumer("@Autowired @Qualifier(\"<caret>\") Pet injected;")
        assertEquals(setOf("dog", "cat"), qualifierReference().variantNames())
    }

    private fun assertParrotImportedAndExcluded() {
        ModificationTrackerManager.getInstance(project).invalidateAll()
        assertTrue(
            "Precondition: Parrot is reached through @Import",
            "com.explyt.demo.Parrot" in PackageScanService.getInstance(project).getAllPackages().importQualified
        )
        assertEquals(
            "Precondition: Parrot is inactive by its profile",
            ProfileActivation.INACTIVE,
            SpringSearchService.getInstance(project).profileActivation(myFixture.findClass("com.explyt.demo.Parrot"))
        )
        val excluded = SpringSearchServiceFacade.getInstance(project).getExcludedBeansClasses(module)
            .mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        assertTrue("Precondition: Parrot is excluded, got $excluded", "com.explyt.demo.Parrot" in excluded)
    }

    private fun configurePetImplementations() {
        myFixture.addFileToProject("application.properties", "spring.profiles.active=prod\nfeature.bird=false")
        myFixture.addFileToProject(
            "com/explyt/demo/App.java",
            "package com.explyt.demo; @org.springframework.boot.autoconfigure.SpringBootApplication public class App {}"
        )
        myFixture.addFileToProject("com/explyt/demo/Pet.java", "package com.explyt.demo; public interface Pet {}")
        myFixture.addFileToProject(
            "com/explyt/demo/Dog.java",
            "package com.explyt.demo; @org.springframework.stereotype.Component public class Dog implements Pet {}"
        )
        myFixture.addFileToProject(
            "com/explyt/demo/Cat.java",
            """
            package com.explyt.demo;
            @org.springframework.stereotype.Component
            @org.springframework.context.annotation.Profile("dev")
            public class Cat implements Pet {}
            """.trimIndent()
        )
        myFixture.addFileToProject(
            "com/explyt/demo/Bird.java",
            """
            package com.explyt.demo;
            @org.springframework.stereotype.Component
            @org.springframework.boot.autoconfigure.condition.ConditionalOnProperty(name = "feature.bird", havingValue = "true")
            public class Bird implements Pet {}
            """.trimIndent()
        )
        ModificationTrackerManager.getInstance(project).invalidateAll()
        val profiles = ProfilesService.getInstance(project)
        assertTrue("Precondition: prod is active", profiles.compute("prod"))
        assertFalse("Precondition: dev is not active", profiles.compute("dev"))
        assertEquals(
            "Precondition: Cat is inactive by its profile",
            ProfileActivation.INACTIVE,
            SpringSearchService.getInstance(project).profileActivation(myFixture.findClass("com.explyt.demo.Cat"))
        )
        val facade = SpringSearchServiceFacade.getInstance(project)
        val active = facade.getAllActiveBeans(module).mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        val excluded = facade.getExcludedBeansClasses(module).mapNotNullTo(mutableSetOf()) { it.psiClass.qualifiedName }
        assertTrue("Precondition: Dog is active, got $active", "com.explyt.demo.Dog" in active)
        assertTrue("Precondition: Cat and Bird are excluded, got $excluded", excluded.containsAll(PET_EXCLUDED))
        assertTrue("Precondition: Cat and Bird are not active, got $active", PET_EXCLUDED.none { it in active })
    }

    private fun configureBeans(animalName: String = "animal"): PsiMethod {
        myFixture.addFileToProject(
            "com/explyt/demo/App.java",
            "package com.explyt.demo; @org.springframework.boot.autoconfigure.SpringBootApplication public class App {}"
        )
        myFixture.addFileToProject("com/explyt/demo/Animal.java", "package com.explyt.demo; public class Animal {}")
        myFixture.addFileToProject("com/explyt/demo/Vehicle.java", "package com.explyt.demo; public class Vehicle {}")
        myFixture.addFileToProject(
            "com/explyt/demo/Beans.java",
            """
            package com.explyt.demo;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            @Configuration(proxyBeanMethods = false)
            public class Beans {
                @Bean("$animalName") public Animal animal() { return new Animal(); }
                @Bean public Vehicle vehicle() { return new Vehicle(); }
            }
            """.trimIndent()
        )
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        val animalBeans = beans.filter { it.name == animalName.replace("\\n", "\n") }
        assertEquals("Precondition: one active animal factory", 1, animalBeans.size)
        assertEquals("com.explyt.demo.Animal", animalBeans.single().psiClass.qualifiedName)
        val vehicleBeans = beans.filter { it.name == "vehicle" }
        assertEquals("Precondition: one active different-type factory", 1, vehicleBeans.size)
        assertEquals("com.explyt.demo.Vehicle", vehicleBeans.single().psiClass.qualifiedName)
        val method = animalBeans.single().psiMember as PsiMethod
        assertEquals("animal", method.name)
        assertEquals("com.explyt.demo.Beans", method.containingClass?.qualifiedName)
        return method
    }

    private fun configureConsumer(body: String) {
        myFixture.configureByText(
            "Consumer.java",
            """
            package com.explyt.demo;
            import org.springframework.stereotype.Component;
            import org.springframework.beans.factory.annotation.Autowired;
            import org.springframework.beans.factory.annotation.Qualifier;
            @Component public class Consumer {
                $body
            }
            """.trimIndent()
        )
    }

    private fun qualifierHost(): PsiLanguageInjectionHost {
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)
        val host = PsiTreeUtil.getParentOfType(leaf, PsiLanguageInjectionHost::class.java, false)
            ?: error("Precondition: string host at caret")
        val annotation = host.toUElement()?.getParentOfType<UAnnotation>()
        assertEquals(
            "Precondition: qualifier annotation resolves",
            "org.springframework.beans.factory.annotation.Qualifier",
            annotation?.resolve()?.qualifiedName
        )
        return host
    }

    private fun qualifierReference(): CompleteBeanReference {
        val host = qualifierHost()
        val references = host.references.filterIsInstance<CompleteBeanReference>()
        assertEquals("Expected one qualifier bean-name reference on ${host.text}", 1, references.size)
        return references.single()
    }

    private fun CompleteBeanReference.variantNames(): Set<String> =
        variants.map { (it as LookupElement).lookupString }.toSet()

    private fun assertResolvesTo(expected: PsiMethod, reference: CompleteBeanReference) {
        val actual = reference.resolve()
        assertTrue(
            "Qualifier must resolve to Beans.animal, got $actual",
            actual != null && psiManager.areElementsEquivalent(expected, actual)
        )
    }

    private companion object {
        val PET_EXCLUDED = setOf("com.explyt.demo.Cat", "com.explyt.demo.Bird")
    }
}
