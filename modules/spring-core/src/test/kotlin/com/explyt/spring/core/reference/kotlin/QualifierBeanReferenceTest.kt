/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.reference.kotlin

import com.explyt.spring.core.references.CompleteBeanReference
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.lookup.LookupElement
import com.intellij.openapi.util.registry.Registry
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiLanguageInjectionHost
import com.intellij.psi.PsiMethod
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.uast.*

class QualifierBeanReferenceTest : ExplytKotlinLightTestCase() {
    override val libraries = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        Registry.get("explyt.spring.root.runConfiguration").setValue(false, testRootDisposable)
    }

    fun testPropertyCompletionOffersOnlyMatchingBeanType() {
        configureBeans()
        configureConsumer("@Autowired @Qualifier(\"<caret>\") lateinit var injected: Animal")
        assertEquals(setOf("animal"), qualifierReference().variantNames())
    }

    fun testPropertyQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("@Autowired @Qualifier(\"anim<caret>al\") lateinit var injected: Animal")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testFieldUseSiteQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("@Autowired @field:Qualifier(\"anim<caret>al\") lateinit var injected: Animal")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testConstructorValQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("", "(@Qualifier(\"anim<caret>al\") val injected: Animal)")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testSetterParameterQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("var injected: Animal? = null\n    @Autowired set(@Qualifier(\"anim<caret>al\") value) { field = value }")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testFunctionParameterQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer("@Autowired fun inject(@Qualifier(\"anim<caret>al\") value: Animal) {}")
        assertResolvesTo(bean, qualifierReference())
    }

    fun testMetaQualifierResolvesToBeanFactory() {
        val bean = configureBeans()
        configureConsumer(
            "@Autowired @AnimalQualifier(\"anim<caret>al\") lateinit var injected: Animal",
            annotationDeclaration = "@Qualifier annotation class AnimalQualifier(val value: String)"
        )
        val host = qualifierHost("com.explyt.demo.AnimalQualifier")
        val metaAnnotation = host.toUElement()?.getParentOfType<UAnnotation>()?.resolve()
            ?.getAnnotation("org.springframework.beans.factory.annotation.Qualifier")
        assertEquals(
            "Precondition: custom annotation is meta-qualified",
            "org.springframework.beans.factory.annotation.Qualifier",
            metaAnnotation?.resolveAnnotationType()?.qualifiedName
        )
        assertResolvesTo(bean, qualifierReference(host))
    }

    fun testInterpolatedTemplateHasNoBeanNameReference() {
        configureBeans("anima")
        configureConsumer(
            "@Autowired @Qualifier(\"${'$'}{PREFIX}<caret>a\") lateinit var injected: Animal",
            annotationDeclaration = "const val PREFIX = \"anim\""
        )
        val host = qualifierHost()
        assertEquals("anima", (host.toUElement() as? UExpression)?.evaluateString())
        assertEquals(emptyList<CompleteBeanReference>(), host.references.filterIsInstance<CompleteBeanReference>())
    }

    fun testEscapedValueReferenceUsesWholeHostValueRange() {
        val bean = configureBeans("ani\\nmal")
        configureConsumer("@Autowired @Qualifier(\"ani\\n<caret>mal\") lateinit var injected: Animal")
        val host = qualifierHost()
        val reference = qualifierReference(host)
        assertEquals(TextRange(1, host.textLength - 1), reference.rangeInElement)
        assertEquals("ani\\nmal", reference.value)
        assertResolvesTo(bean, reference)
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

    private fun configureConsumer(body: String, constructor: String = "", annotationDeclaration: String = "") {
        myFixture.configureByText(
            "Consumer.kt",
            """
            package com.explyt.demo
            import org.springframework.stereotype.Component
            import org.springframework.beans.factory.annotation.Autowired
            import org.springframework.beans.factory.annotation.Qualifier
            $annotationDeclaration
            @Component class Consumer$constructor {
                $body
            }
            """.trimIndent()
        )
    }

    private fun qualifierHost(annotationName: String = "org.springframework.beans.factory.annotation.Qualifier"): PsiLanguageInjectionHost {
        val leaf = myFixture.file.findElementAt(myFixture.caretOffset)
        val host = PsiTreeUtil.getParentOfType(leaf, PsiLanguageInjectionHost::class.java, false)
            ?: error("Precondition: string host at caret")
        val annotation = host.toUElement()?.getParentOfType<UAnnotation>()
        assertEquals("Precondition: qualifier annotation resolves", annotationName, annotation?.resolve()?.qualifiedName)
        return host
    }

    private fun qualifierReference(host: PsiLanguageInjectionHost = qualifierHost()): CompleteBeanReference {
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
}
