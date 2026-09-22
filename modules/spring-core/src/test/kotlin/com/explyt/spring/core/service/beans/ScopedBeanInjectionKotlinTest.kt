/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytKotlinLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiType
import com.intellij.psi.search.GlobalSearchScope

/**
 * The question the tool was built for: will Spring inject this Kotlin parameter that carries a default?
 *
 * Answering it needs two facts that are independent of each other - whether a bean exists, and whether the
 * parameter must be satisfied at all. A missing bean for a defaulted parameter is not a startup failure, and
 * text search over the constructor cannot tell the two apart.
 */
class ScopedBeanInjectionKotlinTest : ExplytKotlinLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.kotlin_1_9_22,
        TestLibrary("org.jetbrains.kotlin:kotlin-reflect:1.9.22")
    )

    /** No bean, but the default makes the parameter optional - so this is not a failure to report. */
    fun testDefaultedParameterWithoutAnyBeanIsOptionalNotBroken() {
        val point = clockPoint()

        val result = ScopedBeanInjectionResolver(project).resolve(snapshotOf(), point)

        assertEquals(BeanOutcome.NO_CANDIDATE, result.outcome)
        assertEquals(false, point.facts.required)
        assertEquals(true, point.facts.hasDefaultValue)
    }

    /** One bean wins over the default: Spring injects it rather than evaluating the default expression. */
    fun testDefaultedParameterWithOneBeanResolves() {
        val point = clockPoint()

        val result = ScopedBeanInjectionResolver(project).resolve(snapshotOf(clock("bean-utc", "utc")), point)

        assertEquals(BeanOutcome.RESOLVED, result.outcome)
        assertEquals(listOf("bean-utc"), result.match.records.map { it.id })
    }

    /** A default does not break a tie: two candidates and nothing to choose by is still ambiguous. */
    fun testDefaultedParameterWithTwoBeansIsAmbiguous() {
        val point = clockPoint()

        val result = ScopedBeanInjectionResolver(project)
            .resolve(snapshotOf(clock("bean-utc", "utc"), clock("bean-fixed", "fixed")), point)

        assertEquals(BeanOutcome.AMBIGUOUS, result.outcome)
        assertEquals(setOf("bean-utc", "bean-fixed"), result.match.records.map { it.id }.toSet())
    }

    /** A nullable parameter is optional for the same reason, and resolves the same way when a bean exists. */
    fun testNullableParameterIsOptionalAndStillResolves() {
        val point = pointAt("clock: Clock?")

        val empty = ScopedBeanInjectionResolver(project).resolve(snapshotOf(), point)
        val withBean = ScopedBeanInjectionResolver(project).resolve(snapshotOf(clock("bean-utc", "utc")), point)

        assertEquals(false, point.facts.required)
        assertEquals(BeanOutcome.NO_CANDIDATE, empty.outcome)
        assertEquals(BeanOutcome.RESOLVED, withBean.outcome)
    }

    /** A plain parameter stays required, or the optional verdicts above would prove nothing. */
    fun testPlainParameterIsRequiredAndReportsTheMissingBean() {
        val point = pointAt("class PlainConsumer(val clock: Clock)", "class PlainConsumer(val ".length)

        val result = ScopedBeanInjectionResolver(project).resolve(snapshotOf(), point)

        assertEquals(true, point.facts.required)
        assertEquals(BeanOutcome.NO_CANDIDATE, result.outcome)
    }

    private fun clockPoint(): SpringInjectionPoint = pointAt("clock: Clock = Clock.systemUTC()")

    private fun pointAt(marker: String, offsetInMarker: Int = 0): SpringInjectionPoint {
        val file = defaultClock()
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val target = offset + offsetInMarker
        val line = document.getLineNumber(target) + 1
        val column = target - document.getLineStartOffset(line - 1) + 1
        return SpringInjectionPointResolver(project).resolve(file, line, column)
    }

    private fun defaultClock(): PsiFile {
        val file = myFixture.copyFileToProject("beanQuery/DefaultClock.kt", "com/explyt/demo/DefaultClock.kt")
        val psiFile = myFixture.psiManager.findFile(file)
        assertNotNull("Precondition: the fixture must load, otherwise nothing is proven", psiFile)
        return psiFile!!
    }

    private fun clock(id: String, name: String) = ScopedBeanRecord(
        id = id,
        name = name,
        knownNames = setOf(name),
        typeName = "java.time.Clock",
        kind = BeanKind.BEAN_METHOD,
        declaration = null,
        declaredType = clockType(),
        declarationModule = null,
        primary = false,
        priority = null,
        details = BeanDetailsEvidence(aliases = listOf(name), primary = false),
        limitations = emptySet()
    )

    private fun snapshotOf(vararg records: ScopedBeanRecord) = ScopedBeanSnapshot(
        application = BeanApplicationIdentity("com.explyt.demo.App", module.name, "app-source"),
        selection = BeanContextSelection(BeanModelSource.STATIC, null, emptySet()),
        modelStamp = "stamp",
        records = records.toList(),
        limitations = emptySet()
    )

    private fun clockType(): PsiType {
        val psiClass = JavaPsiFacade.getInstance(project)
            .findClass("java.time.Clock", GlobalSearchScope.allScope(project))
            ?: error("No PSI for java.time.Clock")
        return JavaPsiFacade.getElementFactory(project).createType(psiClass)
    }
}
