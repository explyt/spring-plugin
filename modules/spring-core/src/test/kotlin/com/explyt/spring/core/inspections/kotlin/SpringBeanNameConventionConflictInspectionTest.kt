/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.inspections.BeanNameConventionConflictFixture
import com.explyt.spring.core.inspections.SpringBeanNameConventionConflictInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo

private const val CONVENTION_NAME = """
    @Bean("fixed")
    annotation class MyBean(val name: Array<String> = [])
"""

private const val MIRROR = """
    @Bean("fixed")
    annotation class MyBean(
        @get:AliasFor("name") val value: Array<String> = [],
        @get:AliasFor("value") val name: Array<String> = [],
    )
"""

private const val SCALAR_NAME_FIXED_BY_VALUE = """
    @Bean("fixed")
    annotation class MyBean(val name: String = "")
"""

private const val NESTED = """
    @Bean("fixed")
    annotation class MyBean(val name: Array<String> = [])

    @MyBean
    annotation class Nested(val name: Array<String> = [])
"""

abstract class SpringBeanNameConventionConflictKotlinTestCase : ExplytInspectionKotlinTestCase() {

    protected abstract val springMajor: Int?

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanNameConventionConflictInspection::class.java)
    }

    protected fun problemsIn(declarations: String, usage: String, usedAnnotation: String = "demo.MyBean"): List<HighlightInfo> {
        myFixture.configureByText(
            "AppConfiguration.kt",
            """
            |package demo
            |import org.springframework.context.annotation.Bean
            |import org.springframework.context.annotation.Configuration
            |import org.springframework.core.annotation.AliasFor
            |${declarations.trimIndent()}
            |@Configuration
            |open class AppConfiguration {
            |    $usage
            |    open fun foo(): Foo = Foo()
            |}
            |class Foo
            """.trimMargin()
        )
        BeanNameConventionConflictFixture.assertPreconditions(myFixture, module, usedAnnotation, springMajor)
        return BeanNameConventionConflictFixture.problems(myFixture)
    }

    protected fun assertConflict(declarations: String, usage: String, usedAnnotation: String = "demo.MyBean") {
        BeanNameConventionConflictFixture.assertSingleConflict(
            problemsIn(declarations, usage, usedAnnotation), usage, usedAnnotation.substringAfterLast('.'), "fixed"
        )
    }

    protected fun assertNoConflict(declarations: String, usage: String, usedAnnotation: String = "demo.MyBean") {
        BeanNameConventionConflictFixture.assertNoConflict(problemsIn(declarations, usage, usedAnnotation))
    }
}

class SpringBeanNameConventionConflictInspectionTest : SpringBeanNameConventionConflictKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)
    override val springMajor: Int = 6

    fun testConventionNameConflictsWithMetaBeanValue() = assertConflict(CONVENTION_NAME, "@MyBean(name = [\"x\"])")

    fun testValueAttributeIsNeverConventionMapped() = assertNoConflict(
        """
        @Bean("fixed")
        annotation class MyBean(val value: Array<String> = [])
        """,
        "@MyBean(value = [\"x\"])",
    )

    fun testConventionNameLeftAtDefaultIsNotAConflict() = assertNoConflict(CONVENTION_NAME, "@MyBean")

    fun testExplicitAliasToBeanNameIsNotAConflict() = assertNoConflict(
        """
        @Bean("fixed")
        annotation class MyBean(@get:AliasFor(annotation = Bean::class, attribute = "name") val name: Array<String> = [])
        """,
        "@MyBean(name = [\"x\"])",
    )

    fun testMetaBeanWithoutFixedNameIsNotAConflict() = assertNoConflict(
        """
        @Bean
        annotation class MyBean(val name: Array<String> = [])
        """,
        "@MyBean(name = [\"x\"])",
    )

    fun testMirrorSetThroughValueIsNotAConflict() = assertNoConflict(MIRROR, "@MyBean(value = [\"x\"])")

    fun testMirrorSetThroughNameConflictsWithMetaBeanValue() = assertConflict(MIRROR, "@MyBean(name = [\"x\"])")

    fun testMirrorLeftAtDefaultIsNotAConflict() = assertNoConflict(MIRROR, "@MyBean")

    fun testConventionNameEqualToMetaBeanValueIsNotAConflict() =
        assertNoConflict(CONVENTION_NAME, "@MyBean(name = [\"fixed\"])")

    fun testConventionNameSetToItsEmptyDefaultIsNotAConflict() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = [])")

    fun testBlankConventionNameConflictsWithMetaBeanValue() = assertConflict(CONVENTION_NAME, "@MyBean(name = [\"\"])")

    fun testScalarConventionNameOverridesMetaBeanName() = assertNoConflict(
        """
        @Bean(name = ["fixed"])
        annotation class MyBean(val name: String = "")
        """,
        "@MyBean(name = \"x\")",
    )

    fun testArrayConventionNameOverridesMetaBeanName() = assertNoConflict(
        """
        @Bean(name = ["fixed"])
        annotation class MyBean(val name: Array<String> = [])
        """,
        "@MyBean(name = [\"x\"])",
    )

    fun testScalarConventionNameConflictsWithMetaBeanValue() =
        assertConflict(SCALAR_NAME_FIXED_BY_VALUE, "@MyBean(name = \"x\")")

    fun testNestedComposedConventionNameConflictsWithMetaBeanValue() =
        assertConflict(NESTED, "@Nested(name = [\"x\"])", "demo.Nested")
}

class SpringBeanNameConventionConflictSpring7InspectionTest : SpringBeanNameConventionConflictKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:7.0.9"))
    override val springMajor: Int = 7

    fun testConventionNameIsIgnoredInSpring7() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = [\"x\"])")

    fun testMirrorSetThroughNameIsIgnoredInSpring7() = assertNoConflict(MIRROR, "@MyBean(name = [\"x\"])")

    fun testBlankConventionNameIsIgnoredInSpring7() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = [\"\"])")

    fun testScalarConventionNameIsIgnoredInSpring7() = assertNoConflict(SCALAR_NAME_FIXED_BY_VALUE, "@MyBean(name = \"x\")")

    fun testNestedComposedConventionNameIsIgnoredInSpring7() =
        assertNoConflict(NESTED, "@Nested(name = [\"x\"])", "demo.Nested")
}

class SpringBeanNameConventionConflictUnknownSpringVersionInspectionTest : SpringBeanNameConventionConflictKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:6.0.7", false))
    override val springMajor: Int? = null

    override fun setUp() {
        super.setUp()
        BeanNameConventionConflictFixture.addSpringCoreWithoutCoordinates(module, testRootDisposable)
    }

    fun testConventionNameIsNotReportedWhenSpringVersionIsUnknown() =
        assertNoConflict(CONVENTION_NAME, "@MyBean(name = [\"x\"])")

    fun testMirrorSetThroughNameIsNotReportedWhenSpringVersionIsUnknown() =
        assertNoConflict(MIRROR, "@MyBean(name = [\"x\"])")
}
