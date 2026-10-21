/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringDuplicateBeanNameInspection
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity
import com.intellij.psi.PsiModifier

private const val CONFIGURATION = "org.springframework.context.annotation.Configuration"
private const val PROFILE = "org.springframework.context.annotation.Profile"

class SpringDuplicateBeanNameRegisteredBaseInspectionTest : ExplytBaseLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_4_0)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDuplicateBeanNameInspection::class.java)
    }

    fun testOverrideWithBeanInRegisteredSubclassOfRegisteredBaseFailsAtStartup() {
        val problems = highlights(baseAnnotations = "@Configuration", overrideAnnotation = "@Bean @Override")

        assertScannedBean("demo.Base")
        assertStartupFailure(problems)
    }

    fun testOverrideWithoutBeanInRegisteredSubclassOfRegisteredBaseFailsAtStartup() {
        val problems = highlights(baseAnnotations = "@Configuration", overrideAnnotation = "@Override")

        assertScannedBean("demo.Base")
        assertStartupFailure(problems)
    }

    fun testOverrideInRegisteredSubclassOfPlainBaseIsNotReported() {
        val problems = highlights(baseAnnotations = "", overrideAnnotation = "@Bean @Override")

        assertFalse("Precondition: demo.Base is not registered", myFixture.findClass("demo.Base").hasAnnotation(CONFIGURATION))
        assertEmpty(problems)
    }

    fun testOverrideOfAbstractConfigurationBaseIsNotReported() {
        val problems = highlights(baseAnnotations = "@Configuration", overrideAnnotation = "@Bean @Override", baseModifiers = "abstract")

        val base = myFixture.findClass("demo.Base")
        assertTrue("Precondition: demo.Base is a @Configuration", base.hasAnnotation(CONFIGURATION))
        assertTrue("Precondition: demo.Base is abstract, so no scan registers it", base.hasModifierProperty(PsiModifier.ABSTRACT))
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    fun testOverrideOfConfigurationBaseOutsideScanRootsIsNotReported() {
        val problems = highlights(baseAnnotations = "@Configuration", overrideAnnotation = "@Bean @Override", basePackage = "outside")

        assertTrue("Precondition: outside.Base is a @Configuration", myFixture.findClass("outside.Base").hasAnnotation(CONFIGURATION))
        assertNotScannedBean("outside.Base")
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    fun testOverrideOfConfigurationBaseWithInactiveProfileIsNotReported() {
        val problems = highlights(baseAnnotations = "@Configuration @Profile(\"off\")", overrideAnnotation = "@Bean @Override")

        val profile = myFixture.findClass("demo.Base").getAnnotation(PROFILE)
        assertEquals("Precondition: demo.Base is active only in profile 'off'", "\"off\"", profile?.findAttributeValue("value")?.text)
        assertNull("Precondition: no profile is activated", myFixture.findFileInTempDir("application.properties"))
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    fun testOverrideInRegisteredSubclassOfRegisteredBaseWithAllowedOverridingIsWeak() {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=true")
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val problems = highlights(baseAnnotations = "@Configuration", overrideAnnotation = "@Bean @Override")

        assertScannedBean("demo.Base")
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
        val weak = problems.filter { it.severity == HighlightSeverity.WEAK_WARNING }
        assertEquals("one weak warning for bean 'animal': ${problems.map { it.text to it.description }}", 1, weak.size)
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.duplicate.overriding.allowed", "animal", "Base.animal"),
            weak.single().description
        )
    }

    fun testOverrideInSubclassWithInactiveProfileOfRegisteredBaseIsNotReported() {
        val problems = highlights(
            baseAnnotations = "@Configuration",
            overrideAnnotation = "@Bean @Override",
            childAnnotations = "@Configuration @Profile(\"off\")",
            childScanned = false,
        )

        val profile = myFixture.findClass("demo.Child").getAnnotation(PROFILE)
        assertEquals("Precondition: demo.Child is active only in profile 'off'", "\"off\"", profile?.findAttributeValue("value")?.text)
        assertNull("Precondition: no profile is activated", myFixture.findFileInTempDir("application.properties"))
        assertInactiveBean("demo.Child")
        assertScannedBean("demo.Base")
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    fun testOverrideInSubclassOutsideScanRootsOfRegisteredBaseIsNotReported() {
        val problems = highlights(
            baseAnnotations = "@Configuration",
            overrideAnnotation = "@Bean @Override",
            childPackage = "outside",
            childScanned = false,
        )

        assertNotScannedBean("outside.Child")
        assertScannedBean("demo.Base")
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    fun testOverrideInAbstractSubclassOfRegisteredBaseIsNotReported() {
        val problems = highlights(
            baseAnnotations = "@Configuration",
            overrideAnnotation = "@Bean @Override",
            childModifiers = "abstract",
            childScanned = false,
        )

        val child = myFixture.findClass("demo.Child")
        assertTrue("Precondition: demo.Child is abstract, so no scan registers it", child.hasModifierProperty(PsiModifier.ABSTRACT))
        assertInactiveBean("demo.Child")
        assertScannedBean("demo.Base")
        assertEmpty(problems.filter { it.severity >= HighlightSeverity.WARNING })
    }

    private fun highlights(
        baseAnnotations: String,
        overrideAnnotation: String,
        basePackage: String = "demo",
        baseModifiers: String = "",
        childAnnotations: String = "@Configuration",
        childPackage: String = "demo",
        childModifiers: String = "",
        childScanned: Boolean = true,
    ): List<HighlightInfo> {
        myFixture.addFileToProject(
            "demo/App.java",
            """
            package demo;

            import org.springframework.boot.autoconfigure.SpringBootApplication;

            @SpringBootApplication
            public class App {}
            """.trimIndent()
        )
        myFixture.addFileToProject("model/Animal.java", "package model;\n\npublic class Animal {}")
        myFixture.addFileToProject("model/Dog.java", "package model;\n\npublic class Dog extends Animal {}")
        myFixture.addFileToProject(
            "$basePackage/Base.java",
            """
            package $basePackage;

            import model.Animal;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Profile;

            $baseAnnotations
            public $baseModifiers class Base {
                @Bean public Animal animal() { return new Animal(); }
            }
            """.trimIndent()
        )
        val child = myFixture.addFileToProject(
            "$childPackage/Child.java",
            """
            package $childPackage;

            import model.Dog;
            import org.springframework.context.annotation.Bean;
            import org.springframework.context.annotation.Configuration;
            import org.springframework.context.annotation.Profile;

            $childAnnotations
            public $childModifiers class Child extends $basePackage.Base {
                $overrideAnnotation public Dog animal() { return new Dog(); }
            }
            """.trimIndent()
        )
        myFixture.configureFromExistingVirtualFile(child.virtualFile)
        val base = myFixture.findClass("$basePackage.Base")
        val childClass = myFixture.findClass("$childPackage.Child")
        assertTrue("Precondition: $childPackage.Child is a @Configuration", childClass.hasAnnotation(CONFIGURATION))
        assertEquals("Precondition: $childPackage.Child extends $basePackage.Base", base, childClass.superClass)
        val inspectionId = SpringDuplicateBeanNameInspection().shortName
        val problems = myFixture.doHighlighting().filter { it.inspectionToolId == inspectionId }
        if (childScanned) assertScannedBean("$childPackage.Child")
        return problems
    }

    private fun activeBeanClasses(): Set<String?> =
        SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
            .filter { it.psiMember == it.psiClass }
            .mapTo(mutableSetOf()) { it.psiClass.qualifiedName }

    private fun assertScannedBean(qualifiedName: String) {
        assertTrue("Precondition: $qualifiedName is inside the application's scan roots", qualifiedName in activeBeanClasses())
    }

    private fun assertNotScannedBean(qualifiedName: String) {
        assertFalse("Precondition: $qualifiedName is outside the application's scan roots", qualifiedName in activeBeanClasses())
    }

    private fun assertInactiveBean(qualifiedName: String) {
        assertFalse("Precondition: $qualifiedName is not an active bean", qualifiedName in activeBeanClasses())
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
