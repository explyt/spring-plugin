/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.codeInsight.daemon.GutterMark
import com.intellij.codeInsight.daemon.LineMarkerInfo
import com.intellij.codeInsight.navigation.NavigationGutterIconRenderer
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiModifier
import com.intellij.psi.search.GlobalSearchScope

class AbstractComponentGutterTest : ExplytKotlinLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testAbstractComponentHasAbstractComponentMarker() {
        configure(
            """
            @Component
            abstract class <caret>AbstractFoo
            @Component class FirstFoo : AbstractFoo()
            @Component class SecondFoo : AbstractFoo()
            @Component class Consumer { @Autowired lateinit var foo: AbstractFoo }
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
        assertActiveBeans("candidates.FirstFoo", "candidates.SecondFoo", "candidates.Consumer")

        val gutter = springGutterAtCaret()
        assertNotSame("An abstract component is not a bean", SpringIcons.SpringBean, gutter.icon)
        assertNotSame("An abstract component is not an inactive bean", SpringIcons.springBeanInactive, gutter.icon)
        assertSame(SpringIcons.SpringBeanDependencies, gutter.icon)
        assertEquals(
            listOf("FirstFoo", "SecondFoo", "foo"),
            SpringGutterTestUtil.getGutterTargetsStrings(gutter).sorted()
        )
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.abstract.component.tooltip", 2),
            gutter.tooltipText
        )
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.abstract.component.popup.title"),
            SpringGutterTestUtil.getGutterPopupTitle(gutter)
        )
    }

    fun testAbstractComponentWithoutImplementationsHasAbstractComponentMarker() {
        configure(
            """
            @Component
            abstract class <caret>AbstractFoo
            @Component class Unrelated
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
        assertActiveBeans("candidates.Unrelated")

        val gutter = springGutterAtCaret()
        assertSame(SpringIcons.SpringBeanDependencies, gutter.icon)
        assertEquals(emptyList<String>(), SpringGutterTestUtil.getGutterTargetsStrings(gutter))
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.abstract.component.tooltip", 0),
            gutter.tooltipText
        )
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.abstract.component.notfound"),
            SpringGutterTestUtil.getGutterEmptyText(gutter)
        )
    }

    fun testAbstractComponentWithLookupKeepsBeanMarker() {
        configure(
            """
            @Component
            abstract class <caret>LookupFoo {
                @org.springframework.beans.factory.annotation.Lookup
                abstract fun bar(): Bar
            }
            @Component class Bar
            @Component class Consumer { @Autowired lateinit var foo: LookupFoo }
            """.trimIndent()
        )
        assertActiveBeans("candidates.LookupFoo", "candidates.Consumer")

        val gutter = springGutterAtCaret()
        assertSame(SpringIcons.SpringBean, gutter.icon)
        assertEquals(listOf("foo"), SpringGutterTestUtil.getGutterTargetsStrings(gutter))
    }

    fun testSingleConstructorInjectionIsAbstractComponentTarget() {
        configure(
            """
            @Component abstract class <caret>AbstractFoo
            @Component class FirstFoo : AbstractFoo()
            @Component class Consumer(private val foo: AbstractFoo)
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
        assertActiveBeans("candidates.FirstFoo", "candidates.Consumer")
        val consumer = JavaPsiFacade.getInstance(project)
            .findClass("candidates.Consumer", GlobalSearchScope.projectScope(project))!!
        val constructor = consumer.constructors.single()
        assertNull(constructor.getAnnotation(SpringCoreClasses.AUTOWIRED))
        assertEquals("foo", constructor.parameterList.parameters.single().name)
        assertEquals(
            listOf("FirstFoo", "foo"),
            SpringGutterTestUtil.getGutterTargetsStrings(springGutterAtCaret()).sorted()
        )
    }

    fun testFactoryBeanCountsAsAbstractComponentImplementation() {
        configureFactoryImplementation()
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.abstract.component.tooltip", 1),
            springGutterAtCaret().tooltipText
        )
    }

    fun testFactoryBeanMethodIsAbstractComponentTarget() {
        configureFactoryImplementation()
        assertEquals(
            listOf("firstFoo()"),
            SpringGutterTestUtil.getGutterTargetsStrings(springGutterAtCaret())
        )
    }

    fun testFactoryBeanOfAbstractTypeIsImplementation() {
        configure(
            """
            @Component abstract class <caret>AbstractFoo
            class FirstFoo : AbstractFoo()
            @org.springframework.context.annotation.Configuration
            open class Cfg {
                @org.springframework.context.annotation.Bean
                open fun foo(): AbstractFoo = FirstFoo()
            }
            """.trimIndent()
        )
        assertActiveBeans("candidates.Cfg")
        val facade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val abstractType = facade.findClass("candidates.AbstractFoo", scope)!!
        val implementation = facade.findClass("candidates.FirstFoo", scope)!!
        assertTrue(abstractType.hasModifierProperty(PsiModifier.ABSTRACT))
        assertTrue(abstractType.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        assertFalse(implementation.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        val factory = facade.findClass("candidates.Cfg", scope)!!.findMethodsByName("foo", false).single()
        assertNotNull(factory.getAnnotation(SpringCoreClasses.BEAN))
        assertEquals(abstractType, (factory.returnType as com.intellij.psi.PsiClassType).resolve())
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        assertFalse("FirstFoo must not be a bean by itself", beans.any { it.psiClass == implementation })
        assertEquals(listOf(factory), beans.filter { it.name == "foo" && it.psiClass == abstractType }.map { it.psiMember })

        val gutter = springGutterAtCaret()
        assertEquals(
            SpringCoreBundle.message("explyt.spring.gutter.abstract.component.tooltip", 1) to listOf("foo()"),
            gutter.tooltipText to SpringGutterTestUtil.getGutterTargetsStrings(gutter)
        )
    }

    private fun configureFactoryImplementation() {
        configure(
            """
            @Component abstract class <caret>AbstractFoo
            class FirstFoo : AbstractFoo()
            @org.springframework.context.annotation.Configuration
            open class Cfg {
                @org.springframework.context.annotation.Bean
                open fun firstFoo(): FirstFoo = FirstFoo()
            }
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
        assertActiveBeans("candidates.Cfg")
        val facade = JavaPsiFacade.getInstance(project)
        val implementation = facade.findClass("candidates.FirstFoo", GlobalSearchScope.projectScope(project))!!
        assertFalse(implementation.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        val factory = facade.findClass("candidates.Cfg", GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("firstFoo", false).single()
        assertNotNull(factory.getAnnotation(SpringCoreClasses.BEAN))
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.name == "firstFoo" && it.psiClass == implementation }
        assertEquals("The subclass must be registered by its factory method", listOf(factory), beans.map { it.psiMember })
    }

    private fun configure(source: String) {
        myFixture.configureByText(
            "Candidates.kt",
            "package candidates\nimport org.springframework.stereotype.Component\n" +
                "import org.springframework.beans.factory.annotation.Autowired\n" + source
        )
        myFixture.doHighlighting()
    }

    private fun springGutterAtCaret(): GutterMark {
        val gutters = myFixture.findGuttersAtCaret().filter {
            (it as? LineMarkerInfo.LineMarkerGutterIconRenderer<*>)?.lineMarkerInfo?.navigationHandler is NavigationGutterIconRenderer
        }
        assertEquals("One Spring gutter on the class line: ${gutters.map { it.tooltipText }}", 1, gutters.size)
        return gutters.single()
    }

    private fun assertAbstractNonBean(qualifiedName: String) {
        val psiClass = JavaPsiFacade.getInstance(project).findClass(qualifiedName, GlobalSearchScope.projectScope(project))
        assertNotNull("$qualifiedName must exist", psiClass)
        assertTrue(psiClass!!.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT))
        assertTrue(psiClass.hasModifierProperty(PsiModifier.ABSTRACT))
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        assertFalse("$qualifiedName must not be a bean", beans.any { it.psiClass.qualifiedName == qualifiedName })
    }

    private fun assertActiveBeans(vararg qualifiedNames: String) {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        qualifiedNames.forEach { qualifiedName ->
            assertTrue("$qualifiedName must be an active bean", beans.any { it.psiClass.qualifiedName == qualifiedName })
        }
    }
}
