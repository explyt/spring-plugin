/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.core.service.SpringSearchService
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil.getGutterTargetsStrings
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiMethod
import com.intellij.psi.search.GlobalSearchScope

class ArrayBeanCovariantOverrideTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    fun testInjectionByOverrideArrayTypeNavigatesToArrayBean() {
        configureCovariantOverride()
        configureConsumer("SubC")
        assertInjectionTargetsArrayBean()
    }

    fun testInjectionByOverrideArrayTypeIsNotReportedAsMissing() {
        configureCovariantOverride()
        configureConsumer("SubC")
        assertNoAutowiringProblems()
    }

    fun testInjectionOfUnproducedArrayTypeIsReportedAsMissing() {
        configureCovariantOverride()
        configureConsumer("Other")
        assertEquals(
            listOf("Autowire failed. No beans of 'List' or 'List<Other[]>' found"),
            autowiringProblems()
        )
    }

    fun testInjectionByDeclaredArrayTypeNavigatesToArrayBean() {
        configureCovariantOverride()
        configureConsumer("C")
        assertInjectionTargetsArrayBean()
        assertNoAutowiringProblems()
    }

    fun testInjectionByDeclaredArrayTypeWithoutOverrideNavigatesToArrayBean() {
        configureConfig(override = false)
        assertEquals(
            "Precondition: Config declares no arrayC override",
            0,
            psiClass("Config").findMethodsByName("arrayC", false).size
        )
        configureConsumer("C")
        assertInjectionTargetsArrayBean()
        assertNoAutowiringProblems()
    }

    fun testObjectFactoryOverriddenByArrayResolvesArrayInjection() {
        myFixture.addFileToProject(
            "com/explyt/demo/Config.java",
            """
            package com.explyt.demo;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            class C {}
            class SubC extends C {}
            class Base {
                @Bean
                public Object thing() { return new Object(); }
            }
            @Configuration
            public class Config extends Base {
                @Override public SubC[] thing() { return new SubC[1]; }
            }
            """.trimIndent()
        )
        assertEquals("java.lang.Object", psiClass("Base").findMethodsByName("thing", false).single().returnType?.canonicalText)
        assertEquals(
            "com.explyt.demo.SubC[]",
            psiClass("Config").findMethodsByName("thing", false).single().returnType?.canonicalText
        )
        configureConsumer("SubC")
        assertInjectionTargetsArrayBean("thing")
        assertNoAutowiringProblems()
    }

    private fun configureCovariantOverride() {
        configureConfig(override = true)
        val base = psiClass("Base").findMethodsByName("arrayC", false).single()
        val override = psiClass("Config").findMethodsByName("arrayC", false).single()
        assertTrue("Precondition: Base.arrayC is @Bean", base.hasAnnotation(SpringCoreClasses.BEAN))
        assertFalse("Precondition: Config.arrayC has no @Bean", override.hasAnnotation(SpringCoreClasses.BEAN))
        assertEquals("com.explyt.demo.C[]", base.returnType?.canonicalText)
        assertEquals("com.explyt.demo.SubC[]", override.returnType?.canonicalText)
        assertTrue("Precondition: Config.arrayC overrides Base.arrayC", override.findSuperMethods().contains(base))

        val arrayBeans = SpringSearchService.getInstance(project).searchArrayComponentPsiClassesByBeanMethods(module)
            .filter { it.name == "arrayC" }
        assertEquals("Precondition: the model holds one arrayC bean", 1, arrayBeans.size)
        assertEquals(
            "Precondition: the arrayC bean is produced by the Config override",
            "Config.arrayC",
            describe(arrayBeans.single().factoryMethod)
        )
    }

    private fun configureConfig(override: Boolean) {
        val overrideMethod = if (override) "@Override public SubC[] arrayC() { return new SubC[1]; }" else ""
        myFixture.addFileToProject(
            "com/explyt/demo/Config.java",
            """
            package com.explyt.demo;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            class C {}
            class SubC extends C {}
            class Other {}
            class Base {
                @Bean
                public C[] arrayC() { return new C[1]; }
            }
            @Configuration
            public class Config extends Base {
                $overrideMethod
            }
            """.trimIndent()
        )
    }

    private fun configureConsumer(elementType: String) {
        myFixture.configureByText(
            "Consumer.java",
            """
            package com.explyt.demo;
            import org.springframework.stereotype.Component;
            import org.springframework.beans.factory.annotation.Autowired;
            @Component
            public class Consumer {
                @Autowired
                java.util.List<$elementType[]> arrays;
            }
            """.trimIndent()
        )
        assertEquals(
            "java.util.List<com.explyt.demo.$elementType[]>",
            psiClass("Consumer").findFieldByName("arrays", false)?.type?.canonicalText
        )
    }

    private fun assertInjectionTargetsArrayBean(beanName: String = "arrayC") {
        val fieldLine = myFixture.editor.document.text.lines().indexOfFirst { it.contains(" arrays;") }
        val targets = myFixture.findAllGutters()
            .filter { gutter ->
                val element = (gutter as? LineMarkerInfo.LineMarkerGutterIconRenderer<*>)
                    ?.lineMarkerInfo?.element ?: return@filter false
                myFixture.editor.document.getLineNumber(element.textRange.startOffset) == fieldLine
            }
            .flatMap { getGutterTargetsStrings(it) }
        assertTrue(
            "The injection gutter must target the $beanName bean, got $targets",
            targets.any { it.startsWith(beanName) }
        )
    }

    private fun assertNoAutowiringProblems() {
        assertEquals("No autowiring problems expected", emptyList<String>(), autowiringProblems())
    }

    private fun autowiringProblems(): List<String> = myFixture.doHighlighting()
        .mapNotNull { it.description }
        .filter { it.startsWith("Autowire failed") }

    private fun describe(method: PsiMethod?): String =
        method?.let { "${it.containingClass?.name}.${it.name}" } ?: "null"

    private fun psiClass(name: String) = JavaPsiFacade.getInstance(project)
        .findClass("com.explyt.demo.$name", GlobalSearchScope.projectScope(project)) ?: error("No PSI for $name")
}
