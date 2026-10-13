/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

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
    public class Config {
        @Bean("svc") Object a() { return new Object(); }
        @Bean("svc") Object b() { return new Object(); }
    }
"""

private const val LITE_CONFIGURATION = """
    @Component
    public class Config {
        @Bean("svc") Object a() { return new Object(); }
        @Bean("svc") Object b() { return new Object(); }
    }
"""

private const val METHOD_NAME_COLLISION = """
    @Configuration
    public class Config {
        @Bean Object svc() { return new Object(); }
        @Bean("svc") Object other() { return new Object(); }
    }
"""

private const val ALIAS_COLLISION = """
    @Configuration
    public class Config {
        @Bean({"a", "svc"}) Object x() { return new Object(); }
        @Bean("svc") Object y() { return new Object(); }
    }
"""

private const val ALIAS_COLLISION_AFTER_PRIMARY_NAME = """
    @Configuration
    public class Config {
        @Bean("svc") Object y() { return new Object(); }
        @Bean({"a", "svc"}) Object x() { return new Object(); }
    }
"""

private const val TWO_ALIASES_COLLISION = """
    @Configuration
    public class Config {
        @Bean({"a", "svc"}) Object x() { return new Object(); }
        @Bean({"b", "svc"}) Object y() { return new Object(); }
    }
"""

private const val CONFIGURATION_WITHOUT_UNIQUE_METHODS = """
    @Configuration(enforceUniqueMethods = false)
    public class Config {
        @Bean("svc") Object a() { return new Object(); }
        @Bean("svc") Object b() { return new Object(); }
    }
"""

private const val COMPOSED_CONFIGURATION = """
    @Configuration
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyConfig {}

    @MyConfig
    public class Config {
        @Bean("svc") Object a() { return new Object(); }
        @Bean("svc") Object b() { return new Object(); }
    }
"""

private const val COMPOSED_CONFIGURATION_WITHOUT_UNIQUE_METHODS = """
    @Configuration(enforceUniqueMethods = false)
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyConfig {}

    @MyConfig
    public class Config {
        @Bean("svc") Object a() { return new Object(); }
        @Bean("svc") Object b() { return new Object(); }
    }
"""

private fun aliasedComposedConfiguration(useSite: String) = """
    @Configuration
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyConfig {
        @AliasFor(annotation = Configuration.class, attribute = "enforceUniqueMethods") boolean unique() default false;
    }

    $useSite
    public class Config {
        @Bean("svc") Object a() { return new Object(); }
        @Bean("svc") Object b() { return new Object(); }
    }
"""

abstract class SpringDuplicateBeanNameJavaTestCase : ExplytBaseLightTestCase() {

    protected abstract val springCoreVersion: String?

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
            "Config.java",
            "package demo;\n\nimport java.lang.annotation.Retention;\n" +
                    "import java.lang.annotation.RetentionPolicy;\n" +
                    "import org.springframework.core.annotation.AliasFor;\n" +
                    "import org.springframework.context.annotation.Bean;\n" +
                    "import org.springframework.context.annotation.Configuration;\n" +
                    "import org.springframework.context.annotation.Profile;\n" +
                    "import org.springframework.stereotype.Component;\n" +
                    "import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;\n\n" + body.trimIndent()
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
        val beanMethods = myFixture.findClass("demo.Config").methods
        assertTrue(
            "Precondition: every method of demo.Config must carry a resolved ${SpringCoreClasses.BEAN}",
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
            "the message says that one of the methods is ignored: $description",
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
            "the message names both outcomes, a startup failure and an ignored method: $description",
            description.contains("fails at startup") && description.contains("ignores one of the two methods")
        )
        assertEquals(SpringCoreBundle.message(REGISTRATION_ORDER_MESSAGE, "svc", firstMethod), description)
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
        const val IGNORED_METHOD_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.ignored"
        const val REGISTRATION_ORDER_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.alias.order"
    }
}

class SpringDuplicateBeanNameInspectionTest : SpringDuplicateBeanNameJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_4_0)
    override val springCoreVersion: String = "6.2.0"

    fun testExplicitNamesOnDifferentMethodsCollide() {
        val problem = highlights(
            """
            @Configuration
            public class Config {
                @Bean("svc") Object a() { return new Object(); }
                @Bean("svc") Object b() { return new Object(); }
            }
            """
        ).single()

        assertProblem(problem, anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testNameAndValueAliasesCollide() {
        val problem = highlights(
            """
            @Configuration
            public class Config {
                @Bean(name = "svc") Object a() { return new Object(); }
                @Bean(value = {"svc"}) Object b() { return new Object(); }
            }
            """
        ).single()

        assertProblem(problem, anchor = "b", beanName = "svc", firstMethod = "a")
    }

    fun testMethodNameCollidesWithExplicitName() {
        val problem = highlights(
            """
            @Configuration
            public class Config {
                @Bean Object svc() { return new Object(); }
                @Bean("svc") Object other() { return new Object(); }
            }
            """
        ).single()

        assertProblem(problem, anchor = "other", beanName = "svc", firstMethod = "svc")
    }

    fun testAnyOfSeveralNamesCollides() {
        val problem = highlights(
            """
            @Configuration
            public class Config {
                @Bean({"a", "svc"}) Object x() { return new Object(); }
                @Bean("svc") Object y() { return new Object(); }
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
                public class Config {
                    @Bean Object svc() { return new Object(); }
                    @Bean Object svc(String dependency) { return new Object(); }
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
                public class Config {
                    @Bean("svc") Object a() { return new Object(); }
                }

                @Configuration
                class OtherConfig {
                    @Bean("svc") Object b() { return new Object(); }
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
                public class Config {
                    @Bean("first") Object a() { return new Object(); }
                    @Bean("second") Object b() { return new Object(); }
                }
                """
            )
        )
    }

    fun testProfileSwitchedPairIsNotReported() {
        val problem = highlights(
            """
            @Configuration
            public class Config {
                @Bean("dataSource") @Profile("dev") Object h2() { return new Object(); }
                @Bean("dataSource") @Profile("prod") Object pg() { return new Object(); }
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
            public class Config {
                @Bean("dataSource") @ConditionalOnProperty(name = "app.dev") Object h2() { return new Object(); }
                @Bean("dataSource") @ConditionalOnProperty(name = "app.prod") Object pg() { return new Object(); }
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
            public class Config {
                @Bean("x") Object plain() { return new Object(); }
                @Bean("x") @Profile("dev") Object conditional() { return new Object(); }
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
            public class Config {
                @Bean("dataSource") @Profile("dev") Object h2() { return new Object(); }
                @Bean("dataSource") @Profile("prod") Object pg() { return new Object(); }
                @Bean("plain") Object first() { return new Object(); }
                @Bean("plain") Object second() { return new Object(); }
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
        allowBeanDefinitionOverriding("\${OVERRIDING}")

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

class SpringDuplicateBeanNameBeforeSpring62InspectionTest : SpringDuplicateBeanNameJavaTestCase() {
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
        allowBeanDefinitionOverriding("\${OVERRIDING}")

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

    fun testAliasCollisionWithAllowedOverridingIsNotReported() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(ALIAS_COLLISION))
    }

    fun testAliasCollisionDeclaredAfterThePrimaryNameFailsAtStartup() {
        assertStartupFailure(highlights(ALIAS_COLLISION_AFTER_PRIMARY_NAME), anchor = "x", firstMethod = "y")
    }

    fun testCollisionOfTwoAliasesFailsAtStartup() {
        assertStartupFailure(highlights(TWO_ALIASES_COLLISION), anchor = "y", firstMethod = "x")
    }
}

class SpringDuplicateBeanNameBeforeSpring6InspectionTest : SpringDuplicateBeanNameJavaTestCase() {
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

    fun testAliasCollisionWithAllowedOverridingIsNotReported() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(ALIAS_COLLISION))
    }

    fun testAliasCollisionWithUnresolvableOverridingPropertyWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        allowBeanDefinitionOverriding("\${OVERRIDING}")

        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testCollisionOfTwoAliasesFailsAtStartup() {
        assertStartupFailure(highlights(TWO_ALIASES_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testMissingOverridingPropertyWarnsThatOneMethodIsIgnored() {
        assertIgnoredMethodWarning(highlights(FULL_CONFIGURATION))
    }
}

class SpringDuplicateBeanNameSpring7InspectionTest : SpringDuplicateBeanNameJavaTestCase() {
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
        allowBeanDefinitionOverriding("\${OVERRIDING}")

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

    fun testAliasCollisionWithAllowedOverridingIsNotReportedInFullConfiguration() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(ALIAS_COLLISION))
    }

    fun testAliasCollisionWithMissingOverridingPropertyFailsAtStartupInFullConfiguration() {
        assertStartupFailure(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    private fun assertSpring7Problem(problems: List<HighlightInfo>) {
        assertSingleProblem(problems, HighlightSeverity.ERROR, anchor = "b", beanName = "svc", firstMethod = "a")
        assertEquals(
            SpringCoreBundle.message("explyt.spring.inspection.bean.name.duplicate.overriding.spring7", "svc", "a"),
            problems.single().description
        )
    }
}

class SpringDuplicateBeanNameUnknownSpringVersionInspectionTest : SpringDuplicateBeanNameJavaTestCase() {
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
        allowBeanDefinitionOverriding("\${OVERRIDING}")

        assertUnknownVersionWarning(highlights(FULL_CONFIGURATION))
    }

    fun testAliasCollisionWithMissingOverridingPropertyWarnsThatTheOutcomeDependsOnRegistrationOrder() {
        assertRegistrationOrderWarning(highlights(ALIAS_COLLISION), anchor = "y", firstMethod = "x")
    }

    fun testAliasCollisionWithAllowedOverridingIsNotReportedInFullConfiguration() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(ALIAS_COLLISION))
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

    fun testAllowedOverridingSuppressesTheProblemInLiteClass() {
        allowBeanDefinitionOverriding("true")

        assertEmpty(highlights(LITE_CONFIGURATION))
    }

    private fun assertUnknownVersionWarning(problems: List<HighlightInfo>) {
        assertSingleProblem(problems, HighlightSeverity.WARNING, anchor = "b", beanName = "svc", firstMethod = "a")
        assertEquals(SpringCoreBundle.message(UNKNOWN_VERSION_MESSAGE, "svc", "a"), problems.single().description)
    }

    private companion object {
        const val UNKNOWN_VERSION_MESSAGE = "explyt.spring.inspection.bean.name.duplicate.version.unknown"
    }
}
