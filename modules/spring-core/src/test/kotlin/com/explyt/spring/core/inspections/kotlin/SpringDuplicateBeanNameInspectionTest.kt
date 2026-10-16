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
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.test.ExplytBaseLightTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.java.library.JavaLibraryUtil
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

private const val METHOD_NAME_COLLISION = """
    @Configuration
    open class Config {
        @Bean fun svc(): Any = Any()
        @Bean("svc") fun other(): Any = Any()
    }
"""

private const val ALIAS_COLLISION = """
    @Configuration
    open class Config {
        @Bean("a", "svc") fun x(): Any = Any()
        @Bean("svc") fun y(): Any = Any()
    }
"""

private const val ALIAS_COLLISION_AFTER_PRIMARY_NAME = """
    @Configuration
    open class Config {
        @Bean("svc") fun y(): Any = Any()
        @Bean("a", "svc") fun x(): Any = Any()
    }
"""

private const val TWO_ALIASES_COLLISION = """
    @Configuration
    open class Config {
        @Bean("a", "svc") fun x(): Any = Any()
        @Bean("b", "svc") fun y(): Any = Any()
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

private const val INHERITED_PRIMARY_NAME_COLLISION = """
    open class Base {
        @Bean open fun a(): Any = Any()
    }

    @Configuration
    open class Config : Base() {
        @Bean("a") open fun b(): Any = Any()
    }
"""

private const val LITE_CHILD_OF_CONFIGURATION_BASE = """
    @Configuration
    open class Base {
        @Bean open fun a(): Any = Any()
    }

    @Component
    open class Config : Base() {
        @Bean("a") open fun b(): Any = Any()
    }
"""

private const val FULL_CHILD_OF_CONFIGURATION_BASE = """
    @Configuration
    open class Base {
        @Bean open fun a(): Any = Any()
    }

    @Configuration
    open class Config : Base() {
        @Bean("a") open fun b(): Any = Any()
    }
"""

abstract class SpringDuplicateBeanNameKotlinTestCase : ExplytBaseLightTestCase() {

    protected abstract val springCoreVersion: String?

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringDuplicateBeanNameInspection::class.java)
    }

    protected fun allowBeanDefinitionOverriding(value: String) {
        myFixture.addFileToProject("application.properties", "spring.main.allow-bean-definition-overriding=$value")
        ModificationTrackerManager.getInstance(project).invalidateAll()
    }

    protected fun highlights(body: String): List<HighlightInfo> = highlightsOf(body) { assertBeanAnnotationResolves() }

    protected fun inheritedHighlights(body: String): List<HighlightInfo> =
        highlightsOf(body) { assertInheritedBeanAnnotationResolves() }

    private fun highlightsOf(body: String, beanPrecondition: () -> Unit): List<HighlightInfo> {
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
            "Precondition: spring-core version of the module",
            springCoreVersion,
            JavaLibraryUtil.getLibraryVersion(module, SpringCoreUtil.SPRING_CORE_MAVEN)
        )
        assertEquals(
            "Precondition: spring-core major version of the module",
            springCoreVersion?.substringBefore('.')?.toInt(),
            SpringBootUtil.getSpringCoreMajorVersion(module)
        )
        assertTrue(
            "Precondition: the inspection is available for the file",
            SpringDuplicateBeanNameInspection().isAvailableForFile(myFixture.file)
        )
        beanPrecondition()
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

    private fun assertInheritedBeanAnnotationResolves() {
        assertNotNull("Precondition: @Bean must come from the fixture library", myFixture.findClass(SpringCoreClasses.BEAN))
        val base = myFixture.findClass("demo.Base")
        val config = myFixture.findClass("demo.Config")
        assertEquals("Precondition: demo.Config extends demo.Base", base, config.superClass)
        val baseFunctions = base.methods.filterNot { it.isConstructor }
        assertTrue(
            "Precondition: every function of demo.Base must carry a resolved ${SpringCoreClasses.BEAN}",
            baseFunctions.isNotEmpty() && baseFunctions.all { it.hasAnnotation(SpringCoreClasses.BEAN) }
        )
        assertTrue(
            "Precondition: every function of demo.Config must carry a resolved ${SpringCoreClasses.BEAN}",
            config.methods.filterNot { it.isConstructor }.all { it.hasAnnotation(SpringCoreClasses.BEAN) }
        )
    }

    protected fun assertInheritedCollision(
        problems: List<HighlightInfo>,
        severity: HighlightSeverity,
        messageKey: String,
        anchor: String,
        beanName: String,
        otherMethod: String
    ) {
        assertSingleProblem(problems, severity, anchor, beanName, otherMethod)
        val description = problems.single().description
        assertFalse(
            "the other function is inherited, so it is not declared 'in this class': $description",
            description.contains("in this class")
        )
        assertEquals(SpringCoreBundle.message(messageKey, beanName, otherMethod), description)
    }

    protected fun assertProblem(problem: HighlightInfo, anchor: String, beanName: String, firstMethod: String) {
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertEquals(anchor, problem.text)
        assertEquals(
            "Bean '$beanName' is also declared by '$firstMethod'; " +
                    "Spring Boot fails at startup unless bean definition overriding is allowed",
            problem.description
        )
    }

    protected fun assertStartupFailure(problems: List<HighlightInfo>, anchor: String, firstMethod: String) {
        assertSingleProblem(problems, HighlightSeverity.ERROR, anchor, beanName = "svc", firstMethod = firstMethod)
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.duplicate", "svc", firstMethod),
            problems.single().description
        )
    }

    protected fun assertIgnoredMethodWarning(
        problems: List<HighlightInfo>,
        anchor: String = "b",
        firstMethod: String = "a"
    ) {
        assertSingleProblem(problems, HighlightSeverity.WARNING, anchor, beanName = "svc", firstMethod = firstMethod)
        val description = problems.single().description
        assertTrue(
            "the message says that one of the functions is ignored: $description",
            description.contains("ignore", ignoreCase = true)
        )
        assertFalse(
            "the context starts below Spring 6.2.0, so no startup failure is claimed: $description",
            description.contains("fails at startup")
        )
        assertEquals(SpringCoreBundle.message(IGNORED_METHOD_MESSAGE, "svc", firstMethod), description)
    }

    protected fun assertRegistrationOrderWarning(problems: List<HighlightInfo>, anchor: String, firstMethod: String) {
        assertSingleProblem(problems, HighlightSeverity.WARNING, anchor, beanName = "svc", firstMethod = firstMethod)
        val description = problems.single().description
        assertTrue(
            "the message names both outcomes, a startup failure and an ignored function: $description",
            description.contains("fails at startup") && description.contains("ignores one of the two methods")
        )
        assertEquals(SpringCoreBundle.message(REGISTRATION_ORDER_MESSAGE, "svc", firstMethod), description)
    }

    protected fun assertOnlyOneMethodRegisteredWarning(
        problems: List<HighlightInfo>,
        anchor: String = "b",
        beanName: String = "svc",
        otherMethod: String = "a"
    ) {
        assertSingleProblem(problems, HighlightSeverity.WEAK_WARNING, anchor, beanName, otherMethod)
        val description = problems.single().description
        assertTrue(
            "the message says that Spring never calls one of the functions: $description",
            description.contains(DEFINITE_OVERRIDING_PHRASE)
        )
        assertFalse(
            "the outcome is definite, so the message is not hedged: $description",
            description.contains(HEDGED_OVERRIDING_PHRASE)
        )
    }

    protected fun assertMayRegisterOnlyOneMethodWarning(
        problems: List<HighlightInfo>,
        anchor: String,
        beanName: String = "svc",
        otherMethod: String
    ) {
        assertSingleProblem(problems, HighlightSeverity.WEAK_WARNING, anchor, beanName, otherMethod)
        val description = problems.single().description
        assertTrue(
            "the message says that Spring may register only one of the functions: $description",
            description.contains(HEDGED_OVERRIDING_PHRASE)
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

    protected companion object {
        const val INSPECTION_ID = "SpringDuplicateBeanName"
        const val STARTUP_FAILURE_MESSAGE = "explyt.spring.inspection.bean.name.duplicate"
        const val IGNORED_METHOD_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.ignored"
        const val REGISTRATION_ORDER_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.alias.order"
        const val SPRING_7_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.overriding.spring7"
        const val INHERITED_METHODS_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.inherited"
        const val DEFINITE_OVERRIDING_PHRASE = "never calls the other"
        const val HEDGED_OVERRIDING_PHRASE = "may register only one"
    }
}

class SpringDuplicateBeanNameInspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_4_0)
    override val springCoreVersion: String = "6.2.0"

    fun testFullSubclassDoesNotRepeatThePrimaryNameWarningOfItsFullBase() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            inheritedHighlights(
                """
                @Configuration
                open class Base {
                    @Bean open fun a(): Any = Any()
                    @Bean("a") open fun b(): Any = Any()
                }

                @Configuration
                open class Config : Base()
                """
            ),
            beanName = "a"
        )
    }

    fun testFullSubclassDoesNotRepeatTheHedgedAliasWarningOfItsFullBase() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(
            inheritedHighlights(
                """
                @Configuration
                open class Base {
                    @Bean("svc") open fun y(): Any = Any()
                    @Bean("a", "svc") open fun x(): Any = Any()
                }

                @Configuration
                open class Config : Base()
                """
            ),
            anchor = "x", otherMethod = "y"
        )
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

    fun testAllowedOverridingWarnsThatSpringRegistersOnlyOneMethod() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(FULL_CONFIGURATION))
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

    fun testAllowedOverridingInLiteClassWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(LITE_CONFIGURATION))
    }

    fun testAliasCollisionWithAllowedOverridingBeforeSpring627WarnsThatSpringMayRegisterOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(highlights(ALIAS_COLLISION), anchor = "y", otherMethod = "x")
    }

    fun testAliasCollisionAfterThePrimaryNameWithAllowedOverridingBeforeSpring627WarnsThatSpringMayRegisterOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(
            highlights(ALIAS_COLLISION_AFTER_PRIMARY_NAME), anchor = "x", otherMethod = "y"
        )
    }

    fun testInheritedMethodNameCollidesWithExplicitNameOfSubclass() {
        assertInheritedCollision(
            inheritedHighlights(INHERITED_PRIMARY_NAME_COLLISION),
            HighlightSeverity.ERROR,
            STARTUP_FAILURE_MESSAGE,
            anchor = "b",
            beanName = "a",
            otherMethod = "Base.a"
        )
    }

    fun testInheritedExplicitNameCollidesWithMethodNameOfSubclass() {
        assertInheritedCollision(
            inheritedHighlights(
                """
                open class Base {
                    @Bean("a") open fun b(): Any = Any()
                }

                @Configuration
                open class Config : Base() {
                    @Bean open fun a(): Any = Any()
                }
                """
            ),
            HighlightSeverity.ERROR,
            STARTUP_FAILURE_MESSAGE,
            anchor = "a",
            beanName = "a",
            otherMethod = "Base.b"
        )
    }

    fun testInheritedAliasCollidesWithPrimaryNameOfSubclass() {
        assertInheritedCollision(
            inheritedHighlights(
                """
                open class Base {
                    @Bean("a", "svc") open fun x(): Any = Any()
                }

                @Configuration
                open class Config : Base() {
                    @Bean("svc") open fun y(): Any = Any()
                }
                """
            ),
            HighlightSeverity.ERROR,
            STARTUP_FAILURE_MESSAGE,
            anchor = "y",
            beanName = "svc",
            otherMethod = "Base.x"
        )
    }

    fun testCollisionBetweenTwoInheritedFunctionsIsReportedOnTheSubclass() {
        val problems = inheritedHighlights(
            """
            open class Base {
                @Bean open fun a(): Any = Any()
                @Bean("a") open fun b(): Any = Any()
            }

            @Configuration
            open class Config : Base()
            """
        ).filter { it.text == "Config" }

        assertEquals("one problem on the subclass name: $problems", 1, problems.size)
        val problem = problems.single()
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertTrue(
            "the message names bean 'a' and both inherited functions: ${problem.description}",
            listOf("'a'", "Base.a", "Base.b").all { problem.description.contains(it) }
        )
        assertFalse(
            "both functions are inherited, so neither is declared 'in this class': ${problem.description}",
            problem.description.contains("in this class")
        )
    }

    fun testBeanFunctionOverriddenInSubclassIsNotACollision() {
        assertEmpty(
            inheritedHighlights(
                """
                open class Base {
                    @Bean open fun a(): Any = Any()
                }

                @Configuration
                open class Config : Base() {
                    @Bean override fun a(): Any = Any()
                }
                """
            )
        )
    }

    fun testCollisionFromDifferentAncestorsIsReportedOnTheSubclass() {
        val problems = inheritedHighlights(
            """
            open class Grandparent {
                @Bean open fun a(): Any = Any()
            }

            open class Base : Grandparent() {
                @Bean("a") open fun b(): Any = Any()
            }

            @Configuration
            open class Config : Base()
            """
        ).filter { it.text == "Config" }

        val grandparent = myFixture.findClass("demo.Grandparent")
        val base = myFixture.findClass("demo.Base")
        assertEquals("Precondition: the two declarations belong to different ancestors", grandparent, base.superClass)
        val grandparentMethods = grandparent.methods.filterNot { it.isConstructor }
        assertEquals("Precondition: the grandparent declares one bean method", 1, grandparentMethods.size)
        assertTrue(grandparentMethods.single().hasAnnotation(SpringCoreClasses.BEAN))
        assertFalse("Precondition: the grandparent is not a component", SpringCoreUtil.run { grandparent.hasComponentAnnotation() })
        assertFalse("Precondition: the parent is not a component", SpringCoreUtil.run { base.hasComponentAnnotation() })
        assertEquals("one problem on the subclass name: $problems", 1, problems.size)
        val problem = problems.single()
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertEquals("Config", problem.text)
        assertEquals(
            SpringCoreBundle.message(
                "explyt.spring.inspection.bean.name.duplicate.inherited",
                "Grandparent.a",
                SpringCoreBundle.message(STARTUP_FAILURE_MESSAGE, "a", "Base.b")
            ),
            problem.description
        )
    }

    fun testCollisionInsideAConfigurationBaseIsNotRepeatedInItsSubclass() {
        val problems = inheritedHighlights(
            """
            @Configuration
            open class Base {
                @Bean open fun a(): Any = Any()
                @Bean("a") open fun b(): Any = Any()
            }

            @Configuration
            open class Config : Base()
            """
        )

        val baseProblems = problems.filter { it.text == "b" }
        assertEquals("the configuration base still reports its own collision", 1, baseProblems.size)
        assertProblem(baseProblems.single(), anchor = "b", beanName = "a", firstMethod = "a")
        assertEquals(
            "the subclass must not repeat the configuration base collision",
            0,
            problems.count { it.text == "Config" }
        )
        assertEquals("only the configuration base reports the collision", 1, problems.size)
    }

    fun testSubclassOfConfigurationWithDifferentNamesIsNotReported() {
        assertEmpty(
            inheritedHighlights(
                """
                @Configuration
                open class Base {
                    @Bean open fun a(): Any = Any()
                }

                @Configuration
                open class Config : Base() {
                    @Bean open fun b(): Any = Any()
                }
                """
            )
        )
    }
}

class SpringDuplicateBeanNameBeforeSpring62InspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)
    override val springCoreVersion: String = "6.0.10"

    fun testMissingOverridingPropertyWarnsThatOneMethodIsIgnored() {
        assertIgnoredMethodWarning(highlights(FULL_CONFIGURATION))
    }

    fun testDisallowedOverridingWarnsThatOneMethodIsIgnored() {
        allowBeanDefinitionOverriding("false")

        assertIgnoredMethodWarning(highlights(FULL_CONFIGURATION))
    }

    fun testAllowedOverridingWarnsThatOneMethodIsIgnored() {
        allowBeanDefinitionOverriding("true")

        assertIgnoredMethodWarning(highlights(FULL_CONFIGURATION))
    }

    fun testUnresolvableOverridingPropertyWarnsThatOneMethodIsIgnored() {
        allowBeanDefinitionOverriding("${'$'}{OVERRIDING}")

        assertIgnoredMethodWarning(highlights(FULL_CONFIGURATION))
    }

    fun testMissingOverridingPropertyWarnsThatOneMethodIsIgnoredInLiteClass() {
        assertIgnoredMethodWarning(highlights(LITE_CONFIGURATION))
    }

    fun testMethodNameCollidingWithExplicitNameWarnsThatOneMethodIsIgnored() {
        assertIgnoredMethodWarning(highlights(METHOD_NAME_COLLISION), anchor = "other", firstMethod = "svc")
    }

    fun testAliasCollisionWithMissingOverridingPropertyFailsAtStartup() {
        assertStartupFailure(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionWithDisallowedOverridingFailsAtStartup() {
        allowBeanDefinitionOverriding("false")

        assertStartupFailure(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionWithAllowedOverridingWarnsThatSpringMayRegisterOnlyOneMethod() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(highlights(ALIAS_COLLISION), anchor = "y", otherMethod = "x")
    }

    fun testAliasCollisionDeclaredAfterThePrimaryNameFailsAtStartup() {
        assertStartupFailure(highlights(ALIAS_COLLISION_AFTER_PRIMARY_NAME), anchor = "x", firstMethod = "y")
    }

    fun testCollisionOfTwoAliasesFailsAtStartup() {
        assertStartupFailure(highlights(TWO_ALIASES_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testInheritedPrimaryNameCollisionWarnsThatOneFunctionIsIgnored() {
        assertInheritedCollision(
            inheritedHighlights(INHERITED_PRIMARY_NAME_COLLISION),
            HighlightSeverity.WARNING,
            IGNORED_METHOD_MESSAGE,
            anchor = "b",
            beanName = "a",
            otherMethod = "Base.a"
        )
    }
}

class SpringDuplicateBeanNameBeforeSpring6InspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_2_7_18)
    override val springCoreVersion: String = "5.3.31"

    fun testAliasCollisionWithMissingOverridingPropertyWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionWithDisallowedOverridingWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        allowBeanDefinitionOverriding("false")

        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionDeclaredAfterThePrimaryNameWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION_AFTER_PRIMARY_NAME), anchor = "x", firstMethod = "y")
    }

    fun testAliasCollisionWithAllowedOverridingWarnsThatSpringMayRegisterOnlyOneMethod() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(highlights(ALIAS_COLLISION), anchor = "y", otherMethod = "x")
    }

    fun testAliasCollisionWithUnresolvableOverridingPropertyWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        allowBeanDefinitionOverriding("${'$'}{OVERRIDING}")

        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testCollisionOfTwoAliasesFailsAtStartup() {
        assertStartupFailure(highlights(TWO_ALIASES_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testMissingOverridingPropertyWarnsThatOneFunctionIsIgnored() {
        assertIgnoredMethodWarning(highlights(FULL_CONFIGURATION))
    }
}

class SpringDuplicateBeanNameSpring7InspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_4_1_0)
    override val springCoreVersion: String = "7.0.8"

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

    fun testComposedConfigurationWithoutUniqueMethodEnforcementWarnsThatSpringRegistersOnlyOneMethod() {
        allowBeanDefinitionOverriding("true")

        assertSpring7Problem(highlights(COMPOSED_CONFIGURATION))
        assertOnlyOneMethodRegisteredWarning(highlights(COMPOSED_CONFIGURATION_WITHOUT_UNIQUE_METHODS))
    }

    fun testComposedConfigurationAliasDefaultWithoutUniqueMethodEnforcementWarnsThatSpringRegistersOnlyOneMethod() {
        allowBeanDefinitionOverriding("true")

        assertSpring7Problem(highlights(aliasedComposedConfiguration("@MyConfig(unique = true)")))
        assertOnlyOneMethodRegisteredWarning(highlights(aliasedComposedConfiguration("@MyConfig")))
    }

    fun testDisallowedOverridingKeepsTheProblemInLiteClass() {
        allowBeanDefinitionOverriding("false")

        assertProblem(highlights(LITE_CONFIGURATION).single(), anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testAllowedOverridingWarnsThatSpringRegistersOnlyOneMethodInLiteClass() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(LITE_CONFIGURATION))
    }

    fun testDisallowedOverridingKeepsTheProblemWithoutUniqueMethodEnforcement() {
        allowBeanDefinitionOverriding("false")

        assertProblem(
            highlights(CONFIGURATION_WITHOUT_UNIQUE_METHODS).single(), anchor = "b", beanName = "svc", firstMethod = "a"
        )
    }

    fun testAllowedOverridingWarnsThatSpringRegistersOnlyOneMethodWithoutUniqueMethodEnforcement() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(CONFIGURATION_WITHOUT_UNIQUE_METHODS))
    }

    fun testAliasCollisionWithAllowedOverridingIsNotReportedInFullConfiguration() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(ALIAS_COLLISION))
    }

    fun testAliasCollisionWithMissingOverridingPropertyFailsAtStartupInFullConfiguration() {
        assertStartupFailure(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionDeclaredAfterThePrimaryNameWithAllowedOverridingWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            highlights(ALIAS_COLLISION_AFTER_PRIMARY_NAME), anchor = "x", otherMethod = "y"
        )
    }

    fun testAllowedOverridingKeepsInheritedCollisionInFullConfiguration() {
        allowBeanDefinitionOverriding("true")

        assertInheritedCollision(
            inheritedHighlights(INHERITED_PRIMARY_NAME_COLLISION),
            HighlightSeverity.ERROR,
            SPRING_7_MESSAGE,
            anchor = "b",
            beanName = "a",
            otherMethod = "Base.a"
        )
    }

    fun testAllowedOverridingWarnsInLiteSubclassOfConfigurationAndKeepsTheInheritedCollisionInTheFullSubclass() {
        allowBeanDefinitionOverriding("true")

        assertInheritedCollision(
            inheritedHighlights(FULL_CHILD_OF_CONFIGURATION_BASE),
            HighlightSeverity.ERROR,
            SPRING_7_MESSAGE,
            anchor = "b",
            beanName = "a",
            otherMethod = "Base.a"
        )
        assertOnlyOneMethodRegisteredWarning(
            inheritedHighlights(LITE_CHILD_OF_CONFIGURATION_BASE), beanName = "a", otherMethod = "Base.a"
        )
    }

    fun testFullSubclassOfLiteBaseKeepsTheSpring7FailureBesideTheWarningOfItsBase() {
        allowBeanDefinitionOverriding("true")

        val problems = inheritedHighlights(
            """
            @Component
            open class Base {
                @Bean open fun a(): Any = Any()
                @Bean("a") open fun b(): Any = Any()
            }

            @Configuration
            open class Config : Base()
            """
        )

        assertOnlyOneMethodRegisteredWarning(problems.filter { it.text != "Config" }, beanName = "a")
        val subclassProblems = problems.filter { it.text == "Config" }
        assertEquals(1, subclassProblems.size)
        val problem = subclassProblems.single()
        assertEquals("Config", problem.text)
        assertEquals(HighlightSeverity.ERROR, problem.severity)
        assertEquals(
            SpringCoreBundle.message(
                "explyt.spring.inspection.bean.name.duplicate.inherited",
                "Base.b",
                SpringCoreBundle.message(SPRING_7_MESSAGE, "a", "Base.a")
            ),
            problem.description
        )
    }

    fun testLiteSubclassOfLiteBaseDoesNotRepeatTheWarningOfItsBase() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            inheritedHighlights(
                """
                @Component
                open class Base {
                    @Bean open fun a(): Any = Any()
                    @Bean("a") open fun b(): Any = Any()
                }

                @Component
                open class Config : Base()
                """
            ),
            beanName = "a"
        )
    }

    private fun assertSpring7Problem(problems: List<HighlightInfo>) {
        assertSingleProblem(problems, HighlightSeverity.ERROR, anchor = "b", beanName = "svc", firstMethod = "a")
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.duplicate.overriding.spring7", "svc", "a"),
            problems.single().description
        )
    }
}

class SpringDuplicateBeanNameSpring627InspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary("org.springframework.boot:spring-boot-autoconfigure:3.5.7"))
    override val springCoreVersion: String = "6.2.12"

    fun testFullSubclassDoesNotRepeatThePrimaryNameWarningOfItsFullBase() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            inheritedHighlights(
                """
                @Configuration
                open class Base {
                    @Bean open fun a(): Any = Any()
                    @Bean("a") open fun b(): Any = Any()
                }

                @Configuration
                open class Config : Base()
                """
            ),
            beanName = "a"
        )
    }

    fun testAllowedOverridingInFullConfigurationWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(FULL_CONFIGURATION))
    }

    fun testAllowedOverridingInLiteClassWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(LITE_CONFIGURATION))
    }

    fun testFunctionNameCollisionWithAllowedOverridingWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            highlights(METHOD_NAME_COLLISION), anchor = "other", otherMethod = "svc"
        )
    }

    fun testAliasCollisionDeclaredAfterThePrimaryNameWithAllowedOverridingWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            highlights(ALIAS_COLLISION_AFTER_PRIMARY_NAME), anchor = "x", otherMethod = "y"
        )
    }

    fun testAliasCollisionDeclaredBeforeThePrimaryNameWithAllowedOverridingIsNotReported() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(ALIAS_COLLISION))
    }

    fun testCollisionOfTwoAliasesWithAllowedOverridingIsNotReported() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(TWO_ALIASES_COLLISION))
    }

    fun testCollisionOfTwoAliasesWithMissingOverridingPropertyFailsAtStartup() {
        assertStartupFailure(highlights(TWO_ALIASES_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testInheritedPrimaryNameCollisionWithAllowedOverridingWarnsThatSpringRegistersOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(
            inheritedHighlights(INHERITED_PRIMARY_NAME_COLLISION), anchor = "b", beanName = "a", otherMethod = "Base.a"
        )
    }

    fun testInheritedAliasCollisionWithAllowedOverridingWarnsThatSpringMayRegisterOnlyOneFunction() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(
            inheritedHighlights(
                """
                open class Base {
                    @Bean("svc") open fun y(): Any = Any()
                }

                @Configuration
                open class Config : Base() {
                    @Bean("a", "svc") open fun x(): Any = Any()
                }
                """
            ),
            anchor = "x",
            otherMethod = "Base.y"
        )
    }

    fun testCollisionBetweenTwoInheritedFunctionsWithAllowedOverridingIsReportedOnTheSubclass() {
        allowBeanDefinitionOverriding("true")

        val problems = inheritedHighlights(
            """
            open class Base {
                @Bean open fun a(): Any = Any()
                @Bean("a") open fun b(): Any = Any()
            }

            @Configuration
            open class Config : Base()
            """
        ).filter { it.text == "Config" }

        assertFalse("Precondition: the base is not a component", SpringCoreUtil.run {
            myFixture.findClass("demo.Base").hasComponentAnnotation()
        })
        assertEquals("one problem on the subclass name: $problems", 1, problems.size)
        val problem = problems.single()
        assertEquals(HighlightSeverity.WEAK_WARNING, problem.severity)
        assertTrue(
            "the message wraps the inherited collision of bean 'a': ${problem.description}",
            problem.description.startsWith(SpringCoreBundle.message(INHERITED_METHODS_MESSAGE, "Base.b", ""))
        )
        assertTrue(
            "the message names bean 'a', both inherited functions and the dropped function: ${problem.description}",
            listOf("'a'", "Base.a", "Base.b", DEFINITE_OVERRIDING_PHRASE).all { problem.description.contains(it) }
        )
    }
}

class SpringDuplicateBeanNameUnknownSpringVersionInspectionTest : SpringDuplicateBeanNameKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:6.0.7", false))
    override val springCoreVersion: String? = null

    override fun setUp() {
        super.setUp()
        BeanNameConventionConflictFixture.addSpringCoreWithoutCoordinates(module, testRootDisposable)
    }

    fun testMissingOverridingPropertyWarnsThatTheVersionIsUnknown() {
        assertUnknownVersionWarning(highlights(FULL_CONFIGURATION))
    }

    fun testDisallowedOverridingWarnsThatTheVersionIsUnknown() {
        allowBeanDefinitionOverriding("false")

        assertUnknownVersionWarning(highlights(FULL_CONFIGURATION))
    }

    fun testUnresolvableOverridingPropertyWarnsThatTheVersionIsUnknown() {
        allowBeanDefinitionOverriding("${'$'}{OVERRIDING}")

        assertUnknownVersionWarning(highlights(FULL_CONFIGURATION))
    }

    fun testAliasCollisionWithMissingOverridingPropertyWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionWithAllowedOverridingWarnsThatSpringMayRegisterOnlyOneMethod() {
        allowBeanDefinitionOverriding("true")

        assertMayRegisterOnlyOneMethodWarning(highlights(ALIAS_COLLISION), anchor = "y", otherMethod = "x")
    }

    fun testCollisionOfTwoAliasesFailsAtStartup() {
        assertStartupFailure(highlights(TWO_ALIASES_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAllowedOverridingInFullConfigurationUsesWarning() {
        allowBeanDefinitionOverriding("true")

        assertSingleProblem(
            highlights(FULL_CONFIGURATION), HighlightSeverity.WARNING, anchor = "b", beanName = "svc", firstMethod = "a"
        )
    }

    fun testAllowedOverridingWarnsThatSpringRegistersOnlyOneMethodInLiteClass() {
        allowBeanDefinitionOverriding("true")

        assertOnlyOneMethodRegisteredWarning(highlights(LITE_CONFIGURATION))
    }

    private fun assertUnknownVersionWarning(problems: List<HighlightInfo>) {
        assertSingleProblem(problems, HighlightSeverity.WARNING, anchor = "b", beanName = "svc", firstMethod = "a")
        assertEquals(SpringCoreBundle.message(UNKNOWN_VERSION_MESSAGE, "svc", "a"), problems.single().description)
    }

    private companion object {
        const val UNKNOWN_VERSION_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.version.unknown"
    }
}
