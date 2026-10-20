/*
 * Copyright (c) 2024 Explyt Ltd
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
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiModifier
import com.intellij.psi.presentation.java.SymbolPresentationUtil
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.psi.KtClassOrObject

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

    fun testGenericFactoryBoundedByAbstractTypeIsImplementation() {
        configure(
            """
            @Component abstract class <caret>AbstractFoo
            class FirstFoo : AbstractFoo()
            @org.springframework.context.annotation.Configuration
            open class Cfg {
                @Suppress("UNCHECKED_CAST")
                @org.springframework.context.annotation.Bean
                open fun <T : AbstractFoo> foo(): T = FirstFoo() as T
            }
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
        assertActiveBeans("candidates.Cfg")
        val facade = JavaPsiFacade.getInstance(project)
        val scope = GlobalSearchScope.projectScope(project)
        val abstractType = facade.findClass("candidates.AbstractFoo", scope)!!
        val factory = facade.findClass("candidates.Cfg", scope)!!.findMethodsByName("foo", false).single()
        assertNotNull(factory.getAnnotation(SpringCoreClasses.BEAN))
        val typeParameter = (factory.returnType as com.intellij.psi.PsiClassType).resolve()
        assertInstanceOf(typeParameter, com.intellij.psi.PsiTypeParameter::class.java)
        assertEquals(listOf(abstractType), typeParameter!!.extendsListTypes.map { it.resolve() })

        assertEquals(listOf("foo()"), SpringGutterTestUtil.getGutterTargetsStrings(springGutterAtCaret()))
    }

    fun testGenericFactoryExcludedByMethodConditionIsNotImplementation() {
        configureConditionalGenericFactory(methodCondition = FEATURE_FOO_CONDITION)
        assertActiveBeans("candidates.Cfg")
        assertExcludedTypedSibling()
        assertConditionalFactory("candidates.Cfg")

        assertEquals(emptyList<String>(), SpringGutterTestUtil.getGutterTargetsStrings(springGutterAtCaret()))
    }

    fun testGenericFactoryOfExcludedConfigurationIsNotImplementation() {
        configureConditionalGenericFactory(classCondition = FEATURE_FOO_CONDITION)
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        assertFalse("Cfg must be excluded by its condition", beans.any { it.psiClass.qualifiedName == "candidates.Cfg" })
        val configuration = JavaPsiFacade.getInstance(project)
            .findClass("candidates.Cfg", GlobalSearchScope.projectScope(project))!!
        assertNotNull(configuration.getAnnotation(CONDITIONAL_ON_PROPERTY))
        assertNotNull(configuration.findMethodsByName("foo", false).single().getAnnotation(SpringCoreClasses.BEAN))

        assertEquals(emptyList<String>(), SpringGutterTestUtil.getGutterTargetsStrings(springGutterAtCaret()))
    }

    fun testGenericFactoryWithMatchingConditionIsImplementation() {
        myFixture.addFileToProject("application.properties", "feature.foo=true")
        configureConditionalGenericFactory(methodCondition = FEATURE_FOO_CONDITION)
        assertActiveBeans("candidates.Cfg")
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        assertTrue("typedFoo must be active when the property matches", beans.any { it.name == "typedFoo" })
        assertConditionalFactory("candidates.Cfg")

        assertEquals(
            listOf("foo()", "typedFoo()"),
            SpringGutterTestUtil.getGutterTargetsStrings(springGutterAtCaret())
        )
    }

    private fun configureConditionalGenericFactory(methodCondition: String = "", classCondition: String = "") {
        configure(
            """
            @Component abstract class <caret>AbstractFoo
            class FirstFoo : AbstractFoo()
            @org.springframework.context.annotation.Configuration
            $classCondition
            open class Cfg {
                @Suppress("UNCHECKED_CAST")
                @org.springframework.context.annotation.Bean
                $methodCondition
                open fun <T : AbstractFoo> foo(): T = FirstFoo() as T

                @org.springframework.context.annotation.Bean
                $methodCondition
                open fun typedFoo(): FirstFoo = FirstFoo()
            }
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
    }

    private fun assertConditionalFactory(configurationName: String) {
        val factory = JavaPsiFacade.getInstance(project)
            .findClass(configurationName, GlobalSearchScope.projectScope(project))!!
            .findMethodsByName("foo", false).single()
        assertNotNull(factory.getAnnotation(SpringCoreClasses.BEAN))
        assertNotNull(factory.getAnnotation(CONDITIONAL_ON_PROPERTY))
        assertInstanceOf(
            (factory.returnType as com.intellij.psi.PsiClassType).resolve(),
            com.intellij.psi.PsiTypeParameter::class.java
        )
    }

    private fun assertExcludedTypedSibling() {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        assertFalse("typedFoo must be excluded by the same condition", beans.any { it.name == "typedFoo" })
    }

    fun testTargetsWithEqualNamesAreOrderedByContainingClass() {
        myFixture.addFileToProject(
            "candidates/ZConsumer.kt",
            """
            package candidates
            import org.springframework.stereotype.Component
            import org.springframework.beans.factory.annotation.Autowired
            @Component class ZConsumer { @Autowired lateinit var foo: AbstractFoo }
            """.trimIndent()
        )
        configure(
            """
            @Component abstract class <caret>AbstractFoo
            @Component class FirstFoo : AbstractFoo()
            @Component class AConsumer { @Autowired lateinit var foo: AbstractFoo }
            """.trimIndent()
        )
        assertAbstractNonBean("candidates.AbstractFoo")
        assertActiveBeans("candidates.FirstFoo", "candidates.ZConsumer", "candidates.AConsumer")
        val scope = GlobalSearchScope.projectScope(project)
        val facade = JavaPsiFacade.getInstance(project)
        listOf("candidates.AConsumer", "candidates.ZConsumer").forEach {
            assertNotNull("$it.foo must exist", facade.findClass(it, scope)!!.findFieldByName("foo", false))
        }

        val targets = gutterTargetsAtCaret()
        assertEquals(
            listOf(
                "FirstFoo" to "candidates.FirstFoo",
                "foo" to "candidates.AConsumer",
                "foo" to "candidates.ZConsumer"
            ),
            targets.map { target ->
                SymbolPresentationUtil.getSymbolPresentableText(target) to
                        PsiTreeUtil.getNonStrictParentOfType(target.navigationElement, KtClassOrObject::class.java)
                            ?.fqName?.asString()
            }
        )
    }

    private fun gutterTargetsAtCaret(): List<PsiElement> {
        val gutter = springGutterAtCaret() as LineMarkerInfo.LineMarkerGutterIconRenderer<*>
        return (gutter.lineMarkerInfo.navigationHandler as NavigationGutterIconRenderer).targetElements
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

    private companion object {
        const val CONDITIONAL_ON_PROPERTY = "org.springframework.boot.autoconfigure.condition.ConditionalOnProperty"
        const val FEATURE_FOO_CONDITION = "@$CONDITIONAL_ON_PROPERTY(name = [\"feature.foo\"], havingValue = \"true\")"
    }

    private fun assertActiveBeans(vararg qualifiedNames: String) {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        qualifiedNames.forEach { qualifiedName ->
            assertTrue("$qualifiedName must be an active bean", beans.any { it.psiClass.qualifiedName == qualifiedName })
        }
    }
}
