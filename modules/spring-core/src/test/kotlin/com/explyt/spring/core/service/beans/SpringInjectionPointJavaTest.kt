/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.beans

import com.explyt.spring.test.ExplytJavaLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.PsiDocumentManager
import com.intellij.psi.PsiFile
import org.junit.Assert

/**
 * Java injection points: which places are answered at all, and what the declaration proves about each shape.
 */
class SpringInjectionPointJavaTest : ExplytJavaLightTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    fun testSoleConstructorOfAComponentNeedsNoAnnotation() {
        val point = resolveAt(forms(), "SingleConsumer(Clock clock)", "SingleConsumer(Clock ".length)

        assertEquals("clock", point.name)
        assertEquals(InjectionShape.SINGLE, point.facts.shape)
        assertEquals(true, point.facts.required)
        assertEquals(false, point.facts.hasDefaultValue)
    }

    fun testOptionalIsNotRequired() {
        val point = resolveAt(forms(), "Optional<Clock> optionalClock", "Optional<Clock> ".length)

        assertEquals(InjectionShape.OPTIONAL, point.facts.shape)
        assertEquals(false, point.facts.required)
        assertEquals("java.time.Clock", point.beanType?.canonicalText)
    }

    fun testCollectionFormsCarryTheirElementType() {
        val file = forms()

        val list = resolveAt(file, "List<Clock> allClocks", "List<Clock> ".length)
        assertEquals(InjectionShape.COLLECTION, list.facts.shape)
        assertEquals("java.time.Clock", list.beanType?.canonicalText)

        val array = resolveAt(file, "Clock[] clockArray", "Clock[] ".length)
        assertEquals(InjectionShape.COLLECTION, array.facts.shape)
        assertEquals("java.time.Clock", array.beanType?.canonicalText)

        val map = resolveAt(file, "Map<String, Clock> clocksByName", "Map<String, Clock> ".length)
        assertEquals(InjectionShape.COLLECTION, map.facts.shape)
        assertEquals("java.time.Clock", map.beanType?.canonicalText)
    }

    /** A map Spring does not populate by bean name is not a collection injection; guessing its form would lie. */
    fun testMapWithNonStringKeyIsUnknown() {
        val point = resolveAt(forms(), "Map<Integer, Clock> clocksByNumber", "Map<Integer, Clock> ".length)

        assertEquals(InjectionShape.UNKNOWN, point.facts.shape)
        assertTrue(InjectionCapabilityPolicy.ELEMENT_TYPE_NOT_PROVEN in point.facts.limitations)
    }

    fun testProviderIsItsOwnShape() {
        val point = resolveAt(forms(), "ObjectProvider<Clock> clockProvider", "ObjectProvider<Clock> ".length)

        assertEquals(InjectionShape.PROVIDER, point.facts.shape)
        assertEquals("java.time.Clock", point.beanType?.canonicalText)
    }

    /** A raw container has no provable element type, so no candidate search may be claimed from it. */
    fun testRawOptionalIsUnknown() {
        val point = resolveAt(forms(), "Optional rawOptional", "Optional ".length)

        assertEquals(InjectionShape.UNKNOWN, point.facts.shape)
        assertNull(point.beanType)
    }

    fun testAutowiredRequiredFalseIsHonoured() {
        val point = resolveAt(forms(), "Clock notRequired", "Clock ".length)

        assertEquals(InjectionShape.SINGLE, point.facts.shape)
        assertEquals(false, point.facts.required)
        assertEquals(InjectionCapabilityPolicy.JAVA_REQUIRED_FALSE, point.facts.basis)
    }

    fun testSetterParametersAreInjectionPoints() {
        val point = resolveAt(forms(), "setClock(Clock injected", "setClock(Clock ".length)

        assertEquals("injected", point.name)
        assertEquals(true, point.facts.required)
    }

    fun testBeanFactoryMethodParameterIsAnInjectionPoint() {
        val point = resolveAt(forms(), "Clock clock(Clock delegate)", "Clock clock(Clock ".length)

        assertEquals("delegate", point.name)
        assertEquals(InjectionShape.SINGLE, point.facts.shape)
    }

    fun testTwoSetterParametersOnOneLineAskForTheColumn() {
        val file = forms()
        val offset = file.text.indexOf("setClock(Clock injected, Clock second)")
        assertTrue("Precondition: the two-parameter setter must exist", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val line = document.getLineNumber(offset) + 1

        val failure = queryProblem { SpringInjectionPointResolver(project).resolve(file, line, null) }

        assertEquals(SpringInjectionPointResolver.INJECTION_POINT_REQUIRED, failure.problem.code)
        assertEquals(setOf("injected", "second"), failure.problem.choices.mapNotNull { it["name"] }.toSet())
    }

    /**
     * Spring picks one of several constructors by rules this model does not evaluate. Naming one would read as
     * a fact, so the place is reported unsupported instead.
     */
    fun testSeveralConstructorsWithoutAutowiredAreUnsupported() {
        val file = forms()
        val offset = file.text.indexOf("TwoConstructors(Clock clock, String name)")
        assertTrue("Precondition: the ambiguous class must exist", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val line = document.getLineNumber(offset) + 1
        val column = offset + "TwoConstructors(Clock ".length - document.getLineStartOffset(line - 1) + 1

        val failure = queryProblem { SpringInjectionPointResolver(project).resolve(file, line, column) }

        assertEquals(SpringInjectionPointResolver.UNSUPPORTED_INJECTION_POINT, failure.problem.code)
    }

    fun testLocalVariableIsNotAnInjectionPoint() {
        val file = forms()
        val offset = file.text.indexOf("Clock local = null")
        assertTrue("Precondition: the local variable must exist", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val line = document.getLineNumber(offset) + 1
        val column = offset + "Clock ".length - document.getLineStartOffset(line - 1) + 1

        val failure = queryProblem { SpringInjectionPointResolver(project).resolve(file, line, column) }

        assertEquals(SpringInjectionPointResolver.UNSUPPORTED_INJECTION_POINT, failure.problem.code)
    }

    fun testLineOutsideTheFileIsRejected() {
        val file = forms()

        val failure = queryProblem { SpringInjectionPointResolver(project).resolve(file, 100_000, null) }

        assertEquals(SpringInjectionPointResolver.INVALID_ARGUMENT, failure.problem.code)
    }

    private fun queryProblem(action: () -> Unit): BeanQueryException =
        Assert.assertThrows(BeanQueryException::class.java) { action() }

    private fun forms(): PsiFile {
        val file = myFixture.copyFileToProject("beanQuery/InjectionForms.java", "com/explyt/demo/InjectionForms.java")
        val psiFile = myFixture.psiManager.findFile(file)
        assertNotNull("Precondition: the fixture must load, otherwise nothing is proven", psiFile)
        return psiFile!!
    }

    private fun resolveAt(file: PsiFile, marker: String, offsetInMarker: Int = 0): SpringInjectionPoint {
        val offset = file.text.indexOf(marker)
        assertTrue("Precondition: marker '$marker' must exist in the fixture", offset >= 0)
        val document = PsiDocumentManager.getInstance(project).getDocument(file)!!
        val target = offset + offsetInMarker
        val line = document.getLineNumber(target) + 1
        val column = target - document.getLineStartOffset(line - 1) + 1
        return SpringInjectionPointResolver(project).resolve(file, line, column)
    }
}
