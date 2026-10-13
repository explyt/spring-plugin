/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * Spring injects through exactly the constructors `AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors`
 * (spring-beans 6.2.19, lines 437-461) returns: the `@Autowired` ones, else the only constructor when it takes
 * parameters, else none - and then `AbstractAutowireCapableBeanFactory.instantiateBean` (line 1240) uses the no-arg one.
 */
class InjectionConstructorRuleTest : ExplytJavaLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springBootAutoConfigure_3_1_1
    )

    fun testOnlyTheAutowiredConstructorIsAnInjectionPointOfTheSourceGutter() {
        configureFixture()
        val targets = sourceGutterTargets()
        assertTrue("The @Autowired constructor parameter must be listed: $targets", "autowiredFoo" in targets)
        assertFalse(
            "determineCandidateConstructors:437-448 keeps only @Autowired constructors, got $targets",
            "plainFoo" in targets
        )
    }

    fun testOnlyTheAutowiredConstructorIsAnInjectionPointOfTheFacade() {
        configureFixture()
        val targets = facadeInjectionPoints()
        assertTrue("The @Autowired constructor parameter must be listed: $targets", "autowiredFoo" in targets)
        assertFalse(
            "determineCandidateConstructors:437-448 keeps only @Autowired constructors, got $targets",
            "plainFoo" in targets
        )
    }

    fun testTheSingleConstructorIsAnInjectionPoint() {
        configureFixture()
        assertTrue("determineCandidateConstructors:450 keeps a single constructor", "singleFoo" in sourceGutterTargets())
        assertTrue("determineCandidateConstructors:450 keeps a single constructor", "singleFoo" in facadeInjectionPoints())
    }

    fun testNoConstructorOfAClassWithSeveralAndNoAutowiredIsAnInjectionPointOfTheSourceGutter() {
        configureFixture()
        val targets = sourceGutterTargets()
        assertFalse(
            "determineCandidateConstructors:461 returns none, instantiateBean:1240 uses the no-arg one, got $targets",
            "ignoredFoo" in targets
        )
    }

    fun testNoConstructorOfAClassWithSeveralAndNoAutowiredIsAnInjectionPointOfTheFacade() {
        configureFixture()
        val targets = facadeInjectionPoints()
        assertFalse(
            "determineCandidateConstructors:461 returns none, instantiateBean:1240 uses the no-arg one, got $targets",
            "ignoredFoo" in targets
        )
    }

    private fun configureFixture() {
        myFixture.configureByText(
            "Candidates.java",
            """
            package candidates;
            import org.springframework.stereotype.Component;
            import org.springframework.beans.factory.annotation.Autowired;
            @Component class Foo {}
            @Component class Bar {}
            @Component class TwoConstructors {
                @Autowired TwoConstructors(Foo autowiredFoo) {}
                TwoConstructors(Foo plainFoo, Bar bar) {}
            }
            @Component class SingleConstructor {
                SingleConstructor(Foo singleFoo) {}
            }
            @Component class NoAutowiredConstructor {
                NoAutowiredConstructor() {}
                NoAutowiredConstructor(Foo ignoredFoo) {}
            }
            """.trimIndent()
        )
        myFixture.doHighlighting()
        assertPreconditions()
    }

    private fun assertPreconditions() {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        listOf("Foo", "TwoConstructors", "SingleConstructor", "NoAutowiredConstructor").forEach { name ->
            assertTrue("candidates.$name must be an active bean", beans.any { it.psiClass.qualifiedName == "candidates.$name" })
        }
        val twoConstructors = findClass("TwoConstructors").constructors
        assertEquals(2, twoConstructors.size)
        assertEquals(
            listOf(true, false),
            twoConstructors.map { it.getAnnotation(SpringCoreClasses.AUTOWIRED) != null }
        )
        val noAutowired = findClass("NoAutowiredConstructor").constructors
        assertEquals(listOf(0, 1), noAutowired.map { it.parameterList.parametersCount })
        assertTrue(noAutowired.none { it.getAnnotation(SpringCoreClasses.AUTOWIRED) != null })
    }

    private fun sourceGutterTargets(): List<String> =
        SpringGutterTestUtil.getGutterTargetString(
            SpringGutterTestUtil.getAllBeanGuttersByIcon(myFixture, SpringIcons.SpringBean)
        ).flatten()

    private fun facadeInjectionPoints(): List<String> {
        val foo = findClass("Foo").toUElementOfType<UClass>()!!
        return SpringSearchServiceFacade.getInstance(project).findInjectionPoints(foo, null, module)
            .map { (it as? PsiNamedElement)?.name ?: it.text }
    }

    private fun findClass(simpleName: String): PsiClass =
        JavaPsiFacade.getInstance(project).findClass("candidates.$simpleName", GlobalSearchScope.projectScope(project))
            ?: throw AssertionError("candidates.$simpleName must exist")
}
