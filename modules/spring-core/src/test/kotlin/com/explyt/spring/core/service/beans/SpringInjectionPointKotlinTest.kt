/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile

/**
 * Kotlin declaration facts at an injection point.
 *
 * The subclass below runs the same cases against a newer Spring, because whether a Kotlin default may be
 * reported as optional depends on the framework version and on Kotlin reflection being on the classpath -
 * not on the declaration alone.
 */
open class SpringInjectionPointKotlinTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.kotlin_1_9_22,
        TestLibrary("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    )

    fun testKotlinDefaultParameterIsOptionalWithItsBasisNamed() {
        val file = defaultClock()

        val point = resolveAt(file, "clock: Clock = Clock.systemUTC()")

        assertEquals("clock", point.name)
        assertEquals(InjectionShape.SINGLE, point.facts.shape)
        assertEquals(true, point.facts.hasDefaultValue)
        assertEquals(false, point.facts.required)
        assertEquals(KOTLIN_DEFAULT_SUPPORTED, point.facts.basis)
    }

    fun testKotlinNullableParameterIsOptional() {
        val file = defaultClock()

        val point = resolveAt(file, "clock: Clock?")

        assertEquals(InjectionShape.SINGLE, point.facts.shape)
        assertEquals(false, point.facts.hasDefaultValue)
        assertEquals(false, point.facts.required)
        assertEquals(InjectionCapabilityPolicy.KOTLIN_NULLABLE_SUPPORTED, point.facts.basis)
    }

    /** A plain non-null parameter must stay required, or the optional verdict above would prove nothing. */
    fun testKotlinPlainParameterStaysRequired() {
        val file = defaultClock()

        val point = resolveAt(file, "class PlainConsumer(val clock: Clock)", "class PlainConsumer(val ".length)

        assertEquals(true, point.facts.required)
        assertEquals(false, point.facts.hasDefaultValue)
    }

    /**
     * Both parameters sit on one line, so a request without a column cannot be answered by picking one: the
     * caller is told the coordinates of each.
     */
    fun testTwoDeclarationsOnOneLineAskForTheColumn() {
        val file = defaultClock()
        val offset = file.text.indexOf("val clock: Clock = Clock.systemUTC(), val other")
        assertTrue("Precondition: the two-parameter fixture must exist", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val line = document.getLineNumber(offset) + 1

        val failure = queryProblem { SpringInjectionPointResolver(project).resolve(file, line, null) }

        assertEquals(SpringInjectionPointResolver.INJECTION_POINT_REQUIRED, failure.problem.code)
        assertEquals(setOf("clock", "other"), failure.problem.choices.mapNotNull { it["name"] }.toSet())
    }

    /** The column must land in the declaration it points at, not in its neighbour on the same line. */
    fun testColumnSelectsTheIntendedDeclaration() {
        val file = defaultClock()

        val point = resolveAt(file, "val other: Clock", "val ".length)

        assertEquals("other", point.name)
    }

    fun testAnnotatedFieldIsAnInjectionPoint() {
        val file = defaultClock()

        val point = resolveAt(file, "lateinit var clock: Clock", "lateinit var ".length)

        assertEquals("clock", point.name)
        assertEquals(InjectionShape.SINGLE, point.facts.shape)
    }

    protected fun queryProblem(action: () -> Unit): BeanQueryException =
        org.junit.Assert.assertThrows(BeanQueryException::class.java) { action() }

    protected fun defaultClock(): PsiFile {
        val file = myFixture.copyFileToProject("beanQuery/DefaultClock.kt", "com/explyt/demo/DefaultClock.kt")
        val psiFile = myFixture.psiManager.findFile(file)
        assertNotNull("Precondition: the fixture must load, otherwise nothing is proven", psiFile)
        return psiFile!!
    }

    /** Coordinates come from the real document, so a fixture edit cannot silently point the test elsewhere. */
    protected fun resolveAt(file: PsiFile, marker: String, offsetInMarker: Int = 0): SpringInjectionPoint {
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val target = offset + offsetInMarker
        val line = document.getLineNumber(target) + 1
        val column = target - document.getLineStartOffset(line - 1) + 1
        return SpringInjectionPointResolver(project).resolve(file, line, column)
    }
}

/**
 * The same cases on a newer Spring. Kotlin optionality is a framework behaviour, not a syntax one, so it is
 * asserted against more than the single version the first fixture pins.
 */
class SpringInjectionPointKotlin62Test : SpringInjectionPointKotlinTest() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:6.2.0"),
        TestLibrary.kotlin_1_9_22,
        TestLibrary("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    )
}

/**
 * Without Kotlin reflection Spring cannot read a default, so the declaration fact survives but requiredness is
 * reported as unknown rather than as optional - a wrong "optional" here would hide a real startup failure.
 */
class SpringInjectionPointKotlinWithoutReflectTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.kotlin_1_9_22
    )

    fun testDefaultWithoutKotlinReflectIsUnknownNotOptional() {
        val file = myFixture.copyFileToProject("beanQuery/DefaultClock.kt", "com/explyt/demo/DefaultClock.kt")
        val psiFile = myFixture.psiManager.findFile(file)!!
        assertNull(
            "Precondition: kotlin-reflect must be absent, otherwise this proves nothing",
            com.intellij.psi.JavaPsiFacade.getInstance(project)
                .findClass("kotlin.reflect.full.KClasses", com.intellij.psi.search.GlobalSearchScope.allScope(project))
        )
        val offset = psiFile.text.indexOf("clock: Clock = Clock.systemUTC()")
        val document = PsiDocumentManager.getInstance(project).getDocument(psiFile)!!
        val line = document.getLineNumber(offset) + 1
        val column = offset - document.getLineStartOffset(line - 1) + 1

        val point = SpringInjectionPointResolver(project).resolve(psiFile, line, column)

        assertEquals(true, point.facts.hasDefaultValue)
        assertNull(point.facts.required)
        assertEquals(InjectionCapabilityPolicy.UNKNOWN_REQUIREDNESS, point.facts.basis)
        assertTrue(InjectionCapabilityPolicy.KOTLIN_REFLECT_MISSING in point.facts.limitations)
    }
}
