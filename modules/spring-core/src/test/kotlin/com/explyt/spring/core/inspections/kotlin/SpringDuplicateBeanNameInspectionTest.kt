/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringDuplicateBeanNameInspection
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity

class SpringDuplicateBeanNameInspectionTest : ExplytBaseLightTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDuplicateBeanNameInspection::class.java)
    }

    fun testExplicitNamesOnDifferentMethodsCollide() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("svc") fun a(): Any = Any()
                @Bean("svc") fun b(): Any = Any()
            }
            """
        ).single()

        assertProblem(problem, anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testNameAndValueAliasesCollide() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean(name = ["svc"]) fun a(): Any = Any()
                @Bean(value = ["svc"]) fun b(): Any = Any()
            }
            """
        ).single()

        assertProblem(problem, anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testMethodNameCollidesWithExplicitName() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean fun svc(): Any = Any()
                @Bean("svc") fun other(): Any = Any()
            }
            """
        ).single()

        assertProblem(problem, anchor = "other", beanName = "svc", firstMethod = "svc")
    }

    fun testAnyOfSeveralNamesCollides() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("a", "svc") fun x(): Any = Any()
                @Bean("svc") fun y(): Any = Any()
            }
            """
        ).single()

        assertProblem(problem, anchor = "y", beanName = "svc", firstMethod = "x")
    }

    fun testOverloadsWithSameMethodNameAreMerged() {
        assertEmpty(
            highlights(
                """
                @Configuration
                open class Config {
                    @Bean fun svc(): Any = Any()
                    @Bean fun svc(dependency: String): Any = Any()
                }
                """
            )
        )
    }

    fun testSameNameInDifferentConfigurationClassesIsOutOfScope() {
        assertEmpty(
            highlights(
                """
                @Configuration
                open class Config {
                    @Bean("svc") fun a(): Any = Any()
                }

                @Configuration
                open class OtherConfig {
                    @Bean("svc") fun b(): Any = Any()
                }
                """
            )
        )
    }

    fun testDifferentNamesDoNotCollide() {
        assertEmpty(
            highlights(
                """
                @Configuration
                open class Config {
                    @Bean("first") fun a(): Any = Any()
                    @Bean("second") fun b(): Any = Any()
                }
                """
            )
        )
    }

    fun testProfileSwitchedPairIsNotReported() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("dataSource") @Profile("dev") fun h2(): Any = Any()
                @Bean("dataSource") @Profile("prod") fun pg(): Any = Any()
            }
            """
        )
        assertConditionalAnnotationResolves("org.springframework.context.annotation.Profile")
        assertTrue("Profile pair must not be reported", problem.isEmpty())
    }

    fun testConditionalOnPropertyPairIsNotReported() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("dataSource") @ConditionalOnProperty(name = ["app.dev"]) fun h2(): Any = Any()
                @Bean("dataSource") @ConditionalOnProperty(name = ["app.prod"]) fun pg(): Any = Any()
            }
            """
        )
        assertConditionalAnnotationResolves("org.springframework.boot.autoconfigure.condition.ConditionalOnProperty")
        assertTrue("ConditionalOnProperty pair must not be reported", problem.isEmpty())
    }

    fun testOnlyOneConditionalMethodIsNotReported() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("x") fun plain(): Any = Any()
                @Bean("x") @Profile("dev") fun conditional(): Any = Any()
            }
            """
        )
        assertConditionalAnnotationResolves("org.springframework.context.annotation.Profile")
        assertTrue("Pair with one conditional method must not be reported", problem.isEmpty())
    }

    fun testPlainPairRemainsReportedAlongsideConditionalPair() {
        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("dataSource") @Profile("dev") fun h2(): Any = Any()
                @Bean("dataSource") @Profile("prod") fun pg(): Any = Any()
                @Bean("plain") fun first(): Any = Any()
                @Bean("plain") fun second(): Any = Any()
            }
            """
        )
        assertConditionalAnnotationResolves("org.springframework.context.annotation.Profile")
        assertTrue(
            "Plain pair must remain reported",
            problem.any { it.text == "second" && it.description.contains("'plain'") }
        )
    }

    fun testAllowedOverridingSuppressesTheProblem() {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=true")
        ModificationTrackerManager.getInstance(project).invalidateAll()

        assertEmpty(
            highlights(
                """
                @Configuration
                open class Config {
                    @Bean("svc") fun a(): Any = Any()
                    @Bean("svc") fun b(): Any = Any()
                }
                """
            )
        )
    }

    fun testUnresolvableOverridingPropertyUsesWarning() {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=${'$'}{OVERRIDING}")
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("svc") fun a(): Any = Any()
                @Bean("svc") fun b(): Any = Any()
            }
            """
        ).single()

        assertEquals(HighlightSeverity.WARNING, problem.severity)
        assertEquals(
            SpringCoreBundle.message(
                "explyt.spring.inspection.bean.name.duplicate.overriding.unresolved",
                "svc",
                "a"
            ),
            problem.description
        )
    }

    fun testDisallowedOverridingKeepsTheProblem() {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=false")
        ModificationTrackerManager.getInstance(project).invalidateAll()

        val problem = highlights(
            """
            @Configuration
            open class Config {
                @Bean("svc") fun a(): Any = Any()
                @Bean("svc") fun b(): Any = Any()
            }
            """
        ).single()

        assertProblem(problem, anchor = "b", beanName = "svc", firstMethod = "a")
    }

    private fun highlights(body: String): List<HighlightInfo> {
        myFixture.configureByText(
            "Config.kt",
"package demo\n\nimport org.springframework.context.annotation.Bean\n" +
                    "import org.springframework.context.annotation.Configuration\n" +
                    "import org.springframework.context.annotation.Profile\n" +
                    "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty\n\n" + body.trimIndent()
        )
        assertBeanAnnotationResolves()
        return myFixture.doHighlighting().filter { it.inspectionToolId == INSPECTION_ID }
    }

    private fun assertConditionalAnnotationResolves(annotation: String) {
        assertNotNull("Precondition: $annotation must come from the fixture library", myFixture.findClass(annotation))
        assertTrue(
            "Precondition: demo.Config must use resolved $annotation",
            myFixture.findClass("demo.Config").methods.any { it.hasAnnotation(annotation) }
        )
    }

    private fun assertBeanAnnotationResolves() {
        assertNotNull("Precondition: @Bean must come from the fixture library", myFixture.findClass(SpringCoreClasses.BEAN))
        val beanMethods = myFixture.findClass("demo.Config").methods.filterNot { it.isConstructor }
        assertTrue(
            "Precondition: every function of demo.Config must carry a resolved ${SpringCoreClasses.BEAN}",
            beanMethods.isNotEmpty() && beanMethods.all { it.hasAnnotation(SpringCoreClasses.BEAN) }
        )
    }

    private fun assertProblem(problem: HighlightInfo, anchor: String, beanName: String, firstMethod: String) {
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertEquals(anchor, problem.text)
        assertEquals(
            "Bean '$beanName' is also declared by '$firstMethod' in this class; " +
                    "Spring Boot fails at startup unless bean definition overriding is allowed",
            problem.description
        )
    }

    private companion object {
        const val INSPECTION_ID = "SpringDuplicateBeanName"
    }
}
