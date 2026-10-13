/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.providers.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.explyt.spring.test.util.SpringGutterTestUtil
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiClass
import com.intellij.psi.PsiNamedElement
import com.intellij.psi.search.GlobalSearchScope
import org.jetbrains.uast.UClass
import org.jetbrains.uast.toUElementOfType

/**
 * Kotlin twin of the Java rule test. `AutowiredAnnotationBeanPostProcessor.determineCandidateConstructors`
 * (spring-beans 6.2.19) keeps an `@Autowired` secondary constructor alone (lines 437-448), keeps a single primary
 * constructor (line 450), and returns none for a no-arg primary plus a secondary constructor, because the primary then
 * equals the default constructor (lines 453-461); `instantiateBean` (line 1240) uses the no-arg primary.
 */
class InjectionConstructorRuleTest : ExplytKotlinLightTestCase() {
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

    fun testTheSinglePrimaryConstructorIsAnInjectionPoint() {
        configureFixture()
        assertTrue("determineCandidateConstructors:450 keeps a single constructor", "singleFoo" in sourceGutterTargets())
        assertTrue("determineCandidateConstructors:450 keeps a single constructor", "singleFoo" in facadeInjectionPoints())
    }

    fun testASecondaryConstructorNextToANoArgPrimaryIsNotAnInjectionPointOfTheSourceGutter() {
        configureFixture()
        val targets = sourceGutterTargets()
        assertFalse(
            "determineCandidateConstructors:461 returns none, instantiateBean:1240 uses the no-arg primary, got $targets",
            "ignoredFoo" in targets
        )
    }

    fun testASecondaryConstructorNextToANoArgPrimaryIsNotAnInjectionPointOfTheFacade() {
        configureFixture()
        val targets = facadeInjectionPoints()
        assertFalse(
            "determineCandidateConstructors:461 returns none, instantiateBean:1240 uses the no-arg primary, got $targets",
            "ignoredFoo" in targets
        )
    }

    fun testThePrimaryConstructorNextToANoArgSecondaryIsAnInjectionPointOfTheSourceGutter() {
        configureFixture()
        val targets = sourceGutterTargets()
        assertTrue(
            "determineCandidateConstructors:453-455 returns {primary, default}; autowireConstructor sorts the greedy " +
                "primary first (ConstructorResolver:209, AutowireUtils:57-59) and uses it, got $targets",
            "primaryFoo" in targets
        )
    }

    fun testThePrimaryConstructorNextToANoArgSecondaryIsAnInjectionPointOfTheFacade() {
        configureFixture()
        val targets = facadeInjectionPoints()
        assertTrue(
            "determineCandidateConstructors:453-455 returns {primary, default}; autowireConstructor sorts the greedy " +
                "primary first (ConstructorResolver:209, AutowireUtils:57-59) and uses it, got $targets",
            "primaryFoo" in targets
        )
    }

    private fun configureFixture() {
        myFixture.configureByText(
            "Candidates.kt",
            """
            package candidates
            import org.springframework.stereotype.Component
            import org.springframework.beans.factory.annotation.Autowired
            @Component class Foo
            @Component class Bar
            @Component class TwoConstructors(plainFoo: Foo, bar: Bar) {
                @Autowired constructor(autowiredFoo: Foo) : this(autowiredFoo, Bar())
            }
            @Component class SingleConstructor(singleFoo: Foo)
            @Component class NoAutowiredConstructor() {
                constructor(ignoredFoo: Foo) : this()
            }
            @Component class PrimaryWithNoArgSecondary(primaryFoo: Foo) {
                constructor() : this(Foo())
            }
            """.trimIndent()
        )
        myFixture.doHighlighting()
        assertPreconditions()
    }

    private fun assertPreconditions() {
        val beans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        listOf("Foo", "TwoConstructors", "SingleConstructor", "NoAutowiredConstructor", "PrimaryWithNoArgSecondary").forEach { name ->
            assertTrue("candidates.$name must be an active bean", beans.any { it.psiClass.qualifiedName == "candidates.$name" })
        }
        val twoConstructors = findClass("TwoConstructors").constructors
        assertEquals(
            "Exactly the one-parameter constructor is @Autowired",
            mapOf(1 to true, 2 to false),
            twoConstructors.associate { it.parameterList.parametersCount to (it.getAnnotation(SpringCoreClasses.AUTOWIRED) != null) }
        )
        val noAutowired = findClass("NoAutowiredConstructor").constructors
        assertEquals(setOf(0, 1), noAutowired.map { it.parameterList.parametersCount }.toSet())
        assertTrue(noAutowired.none { it.getAnnotation(SpringCoreClasses.AUTOWIRED) != null })
        val primaryWithSecondary = findClass("PrimaryWithNoArgSecondary").constructors
        assertEquals(
            "A one-parameter primary and a no-arg secondary, both without @Autowired",
            setOf(0, 1),
            primaryWithSecondary.map { it.parameterList.parametersCount }.toSet()
        )
        assertEquals(2, primaryWithSecondary.map { it.navigationElement }.toSet().size)
        assertTrue(primaryWithSecondary.none { it.getAnnotation(SpringCoreClasses.AUTOWIRED) != null })
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
