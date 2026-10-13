/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.BeanNameConventionConflictFixture
import com.explyt.spring.core.inspections.SpringBeanNameConventionConflictInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.codeInsight.daemon.impl.HighlightInfo

private const val CONVENTION_NAME = """
    @Bean("fixed")
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyBean {
        String[] name() default {};
    }
"""

private const val MIRROR = """
    @Bean("fixed")
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyBean {
        @AliasFor("name") String[] value() default {};
        @AliasFor("value") String[] name() default {};
    }
"""

private const val SCALAR_NAME_FIXED_BY_VALUE = """
    @Bean("fixed")
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyBean {
        String name() default "";
    }
"""

private const val NESTED = """
    @Bean("fixed")
    @Retention(RetentionPolicy.RUNTIME)
    @interface MyBean {
        String[] name() default {};
    }

    @MyBean
    @Retention(RetentionPolicy.RUNTIME)
    @interface Nested {
        String[] name() default {};
    }
"""

abstract class SpringBeanNameConventionConflictJavaTestCase : ExplytInspectionJavaTestCase() {

    protected abstract val springMajor: Int?

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanNameConventionConflictInspection::class.java)
    }

    protected fun problemsIn(declarations: String, usage: String, usedAnnotation: String = "demo.MyBean"): List<HighlightInfo> {
        myFixture.configureByText(
            "AppConfiguration.java",
            """
            |package demo;
            |import org.springframework.context.annotation.Bean;
            |import org.springframework.context.annotation.Configuration;
            |import org.springframework.core.annotation.AliasFor;
            |import java.lang.annotation.Retention;
            |import java.lang.annotation.RetentionPolicy;
            |${declarations.trimIndent()}
            |@Configuration
            |public class AppConfiguration {
            |    $usage
            |    public Foo foo() { return new Foo(); }
            |}
            |class Foo {}
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

class SpringBeanNameConventionConflictInspectionTest : SpringBeanNameConventionConflictJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)
    override val springMajor: Int = 6

    fun testConventionNameConflictsWithMetaBeanValue() = assertConflict(CONVENTION_NAME, "@MyBean(name = \"x\")")

    fun testValueAttributeIsNeverConventionMapped() = assertNoConflict(
        """
        @Bean("fixed")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            String[] value() default {};
        }
        """,
        "@MyBean(value = \"x\")",
    )

    fun testConventionNameLeftAtDefaultIsNotAConflict() = assertNoConflict(CONVENTION_NAME, "@MyBean")

    fun testExplicitAliasToBeanNameIsNotAConflict() = assertNoConflict(
        """
        @Bean("fixed")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            @AliasFor(annotation = Bean.class, attribute = "name") String[] name() default {};
        }
        """,
        "@MyBean(name = \"x\")",
    )

    fun testMetaBeanWithoutFixedNameIsNotAConflict() = assertNoConflict(
        """
        @Bean
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            String[] name() default {};
        }
        """,
        "@MyBean(name = \"x\")",
    )

    fun testMirrorSetThroughValueIsNotAConflict() = assertNoConflict(MIRROR, "@MyBean(value = \"x\")")

    fun testMirrorSetThroughNameConflictsWithMetaBeanValue() = assertConflict(MIRROR, "@MyBean(name = \"x\")")

    fun testMirrorLeftAtDefaultIsNotAConflict() = assertNoConflict(MIRROR, "@MyBean")

    fun testConventionNameEqualToMetaBeanValueIsNotAConflict() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = \"fixed\")")

    fun testConventionNameSetToItsEmptyDefaultIsNotAConflict() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = {})")

    fun testBlankConventionNameConflictsWithMetaBeanValue() = assertConflict(CONVENTION_NAME, "@MyBean(name = \"\")")

    fun testScalarConventionNameOverridesMetaBeanName() = assertNoConflict(
        """
        @Bean(name = "fixed")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            String name() default "";
        }
        """,
        "@MyBean(name = \"x\")",
    )

    fun testArrayConventionNameOverridesMetaBeanName() = assertNoConflict(
        """
        @Bean(name = "fixed")
        @Retention(RetentionPolicy.RUNTIME)
        @interface MyBean {
            String[] name() default {};
        }
        """,
        "@MyBean(name = \"x\")",
    )

    fun testScalarConventionNameConflictsWithMetaBeanValue() =
        assertConflict(SCALAR_NAME_FIXED_BY_VALUE, "@MyBean(name = \"x\")")

    fun testNestedComposedConventionNameConflictsWithMetaBeanValue() =
        assertConflict(NESTED, "@Nested(name = \"x\")", "demo.Nested")
}

class SpringBeanNameConventionConflictSpring7InspectionTest : SpringBeanNameConventionConflictJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:7.0.9"))
    override val springMajor: Int = 7

    fun testConventionNameIsIgnoredInSpring7() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = \"x\")")

    fun testMirrorSetThroughNameIsIgnoredInSpring7() = assertNoConflict(MIRROR, "@MyBean(name = \"x\")")

    fun testBlankConventionNameIsIgnoredInSpring7() = assertNoConflict(CONVENTION_NAME, "@MyBean(name = \"\")")

    fun testScalarConventionNameIsIgnoredInSpring7() = assertNoConflict(SCALAR_NAME_FIXED_BY_VALUE, "@MyBean(name = \"x\")")

    fun testNestedComposedConventionNameIsIgnoredInSpring7() =
        assertNoConflict(NESTED, "@Nested(name = \"x\")", "demo.Nested")
}

class SpringBeanNameConventionConflictUnknownSpringVersionInspectionTest : SpringBeanNameConventionConflictJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary("org.springframework:spring-context:6.0.7", false))
    override val springMajor: Int? = null

    override fun setUp() {
        super.setUp()
        BeanNameConventionConflictFixture.addSpringCoreWithoutCoordinates(module, testRootDisposable)
    }

    fun testConventionNameIsNotReportedWhenSpringVersionIsUnknown() =
        assertNoConflict(CONVENTION_NAME, "@MyBean(name = \"x\")")

    fun testMirrorSetThroughNameIsNotReportedWhenSpringVersionIsUnknown() =
        assertNoConflict(MIRROR, "@MyBean(name = \"x\")")
}
