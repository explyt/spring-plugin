/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringDuplicateBeanNameInspection
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity

private const val CONFIGURATION = "org.springframework.context.annotation.Configuration"

class SpringDuplicateBeanNameRegisteredBaseInspectionTest : ExplytBaseLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_4_0)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDuplicateBeanNameInspection::class.java)
    }

    fun testOverrideWithBeanInRegisteredSubclassOfRegisteredBaseFailsAtStartup() {
        assertStartupFailure(highlights(baseAnnotation = "@Configuration", overrideAnnotation = "@Bean @Override"))
    }

    fun testOverrideWithoutBeanInRegisteredSubclassOfRegisteredBaseFailsAtStartup() {
        assertStartupFailure(highlights(baseAnnotation = "@Configuration", overrideAnnotation = "@Override"))
    }

    fun testOverrideInRegisteredSubclassOfPlainBaseIsNotReported() {
        val problems = highlights(baseAnnotation = "", overrideAnnotation = "@Bean @Override")

        assertFalse("Precondition: demo.Base is not registered", myFixture.findClass("demo.Base").hasAnnotation(CONFIGURATION))
        assertEmpty(problems)
    }

    fun testOverrideInRegisteredSubclassOfRegisteredBaseWithAllowedOverridingIsNotAnError() {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=true")
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val problems = highlights(baseAnnotation = "@Configuration", overrideAnnotation = "@Bean @Override")

        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    private fun highlights(baseAnnotation: String, overrideAnnotation: String): List<HighlightInfo> {
        myFixture.configureByText(
            "Child.java",
            """
            package demo;

            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;

            class Animal {}

            class Dog extends Animal {}

            $baseAnnotation
            class Base {
                @Bean public Animal animal() { return new Animal(); }
            }

            @Configuration
            public class Child extends Base {
                $overrideAnnotation public Dog animal() { return new Dog(); }
            }
            """.trimIndent()
        )
        assertTrue("Precondition: demo.Child is a @Configuration", myFixture.findClass("demo.Child").hasAnnotation(CONFIGURATION))
        assertEquals("Precondition: demo.Child extends demo.Base", myFixture.findClass("demo.Base"), myFixture.findClass("demo.Child").superClass)
        val inspectionId = SpringDuplicateBeanNameInspection().shortName
        return myFixture.doHighlighting().filter { it.inspectionToolId == inspectionId }
    }

    private fun assertStartupFailure(problems: List<HighlightInfo>) {
        assertTrue("Precondition: demo.Base is a @Configuration", myFixture.findClass("demo.Base").hasAnnotation(CONFIGURATION))
        val errors = problems.filter { it.severity == HighlightSeverity.ERROR }
        assertEquals("one startup failure for bean 'animal': ${problems.map { it.text to it.description }}", 1, errors.size)
        val error = errors.single()
        assertTrue("reported on the overriding method or the subclass: ${error.text}", error.text in setOf("animal", "Child"))
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.duplicate", "animal", "Base.animal"),
            error.description
        )
    }
}
