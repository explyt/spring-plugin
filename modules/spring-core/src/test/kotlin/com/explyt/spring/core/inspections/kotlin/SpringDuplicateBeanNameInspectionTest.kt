/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.BeanNameConventionConflictFixture
import com.explyt.spring.core.inspections.SpringDuplicateBeanNameInspection
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.SpringBootUtil
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.lang.annotation.HighlightSeverity

private const val FULL_CONFIGURATION = """
    @Configuration
    open class Config {
        @Bean("svc") fun a(): Any = Any()
        @Bean("svc") fun b(): Any = Any()
    }
"""

private const val LITE_CONFIGURATION = """
    @Component
    open class Config {
        @Bean("svc") fun a(): Any = Any()
        @Bean("svc") fun b(): Any = Any()
    }
"""

private const val CONFIGURATION_WITHOUT_UNIQUE_METHODS = """
    @Configuration(enforceUniqueMethods = false)
    open class Config {
        @Bean("svc") fun a(): Any = Any()
        @Bean("svc") fun b(): Any = Any()
    }
"""

private const val COMPOSED_CONFIGURATION = """
    @Configuration
    annotation class MyConfig

    @MyConfig
    open class Config {
        @Bean("svc") fun a(): Any = Any()
        @Bean("svc") fun b(): Any = Any()
    }
"""

private const val COMPOSED_CONFIGURATION_WITHOUT_UNIQUE_METHODS = """
    @Configuration(enforceUniqueMethods = false)
    annotation class MyConfig

    @MyConfig
    open class Config {
        @Bean("svc") fun a(): Any = Any()
        @Bean("svc") fun b(): Any = Any()
    }
"""

private fun aliasedComposedConfiguration(useSite: String) = """
    @Configuration
    annotation class MyConfig(
        @get:AliasFor(annotation = Configuration::class, attribute = "enforceUniqueMethods") val unique: Boolean = false,
    )

    $useSite
    open class Config {
        @Bean("svc") fun a(): Any = Any()
        @Bean("svc") fun b(): Any = Any()
    }
"""

abstract class SpringDuplicateBeanNameKotlinTestCase : ExplytBaseLightTestCase() {

    protected abstract val springMajor: Int?

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDuplicateBeanNameInspection::class.java)
    }

    protected fun allowBeanDefinitionOverriding(value: String) {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=$value")
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    protected fun highlights(body: String): List<HighlightInfo> {
        myFixture.configureByText(
            "Config.kt",
            "package demo\n\nimport org.springframework.core.annotation.AliasFor\n" +
                    "import org.springframework.context.annotation.Bean\n" +
                    "import org.springframework.context.annotation.Configuration\n" +
                    "import org.springframework.context.annotation.Profile\n" +
                    "import org.springframework.stereotype.Component\n" +
                    "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty\n\n" + body.trimIndent()
        )
        assertEquals(
            "Precondition: spring-core major version of the module",
            springMajor,
            SpringBootUtil.getSpringCoreMajorVersion(module)
        )
        assertTrue(
            "Precondition: the inspection is available for the file",
            SpringDuplicateBeanNameInspection().isAvailableForFile(myFixture.file)
        )
        assertBeanAnnotationResolves()
        return myFixture.doHighlighting().filter { it.inspectionToolId == INSPECTION_ID }
    }

    protected fun assertConditionalAnnotationResolves(annotation: String) {
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

    protected fun assertProblem(problem: HighlightInfo, anchor: String, beanName: String, firstMethod: String) {
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertEquals(anchor, problem.text)
        assertEquals(
            "Bean '$beanName' is also declared by '$firstMethod' in this class; " +
                    "Spring Boot fails at startup unless bean definition overriding is allowed",
            problem.description
        )
    }

    protected fun assertSingleProblem(
        problems: List<HighlightInfo>,
        severity: HighlightSeverity,
        anchor: String,
        beanName: String,
        firstMethod: String
    ) {
        assertEquals("one problem: $problems", 1, problems.size)
        val problem = problems.single()
        assertEquals(severity, problem.severity)
        assertEquals(anchor, problem.text)
        assertTrue(
            "the message names bean '$beanName' and method '$firstMethod': ${problem.description}",
            problem.description.contains("'$beanName'") && problem.description.contains("'$firstMethod'")
        )
    }

    private companion object {
        const val INSPECTION_ID = "SpringDuplicateBeanName"
    }
}

class SpringDuplicateBeanNameInspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)
    override val springMajor: Int = 6

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
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(FULL_CONFIGURATION))
    }

    fun testUnresolvableOverridingPropertyUsesWarning() {
        allowBeanDefinitionOverriding("${'$'}{OVERRIDING}")

        val problem = highlights(FULL_CONFIGURATION).single()

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
        allowBeanDefinitionOverriding("false")

        val problem = highlights(FULL_CONFIGURATION).single()

        assertProblem(problem, anchor = "b", beanName = "svc", firstMethod = "a")
    }
}

class SpringDuplicateBeanNameSpring7InspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)
    override val springMajor: Int = 7

    fun testMissingOverridingPropertyKeepsTheProblemInFullConfiguration() {
        assertSpring7Problem(highlights(FULL_CONFIGURATION))
    }

    fun testDisallowedOverridingKeepsTheProblemInFullConfiguration() {
        allowBeanDefinitionOverriding("false")

        assertSpring7Problem(highlights(FULL_CONFIGURATION))
    }

    fun testAllowedOverridingKeepsTheProblemInFullConfiguration() {
        allowBeanDefinitionOverriding("true")

        assertSpring7Problem(highlights(FULL_CONFIGURATION))
    }

    fun testUnresolvableOverridingPropertyKeepsTheErrorInFullConfiguration() {
        allowBeanDefinitionOverriding("${'$'}{OVERRIDING}")

        assertSpring7Problem(highlights(FULL_CONFIGURATION))
    }

    fun testComposedConfigurationWithoutUniqueMethodEnforcementSuppressesTheProblem() {
        allowBeanDefinitionOverriding("true")

        assertSpring7Problem(highlights(COMPOSED_CONFIGURATION))
        assertEmpty(highlights(COMPOSED_CONFIGURATION_WITHOUT_UNIQUE_METHODS))
    }

    fun testComposedConfigurationAliasDefaultWithoutUniqueMethodEnforcementSuppressesTheProblem() {
        allowBeanDefinitionOverriding("true")

        assertSpring7Problem(highlights(aliasedComposedConfiguration("@MyConfig(unique = true)")))
        assertEmpty(highlights(aliasedComposedConfiguration("@MyConfig")))
    }

    fun testDisallowedOverridingKeepsTheProblemInLiteClass() {
        allowBeanDefinitionOverriding("false")

        assertProblem(highlights(LITE_CONFIGURATION).single(), anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testAllowedOverridingSuppressesTheProblemInLiteClass() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(LITE_CONFIGURATION))
    }

    fun testDisallowedOverridingKeepsTheProblemWithoutUniqueMethodEnforcement() {
        allowBeanDefinitionOverriding("false")

        assertProblem(
            highlights(CONFIGURATION_WITHOUT_UNIQUE_METHODS).single(), anchor = "b", beanName = "svc", firstMethod = "a"
        )
    }

    fun testAllowedOverridingSuppressesTheProblemWithoutUniqueMethodEnforcement() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(CONFIGURATION_WITHOUT_UNIQUE_METHODS))
    }

    private fun assertSpring7Problem(problems: List<HighlightInfo>) {
        assertSingleProblem(problems, HighlightSeverity.ERROR, anchor = "b", beanName = "svc", firstMethod = "a")
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.duplicate.overriding.spring7", "svc", "a"),
            problems.single().description
        )
    }
}

class SpringDuplicateBeanNameUnknownSpringVersionInspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:6.0.7", false))
    override val springMajor: Int? = null

    override fun setUp() {
        super.setUp()
        BeanNameConventionConflictFixture.addSpringCoreWithoutCoordinates(module, testRootDisposable)
    }

    fun testDisallowedOverridingKeepsTheProblem() {
        allowBeanDefinitionOverriding("false")

        assertProblem(highlights(FULL_CONFIGURATION).single(), anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testAllowedOverridingInFullConfigurationUsesWarning() {
        allowBeanDefinitionOverriding("true")

        assertSingleProblem(
            highlights(FULL_CONFIGURATION), HighlightSeverity.WARNING, anchor = "b", beanName = "svc", firstMethod = "a"
        )
    }

    fun testAllowedOverridingSuppressesTheProblemInLiteClass() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(LITE_CONFIGURATION))
    }
}
