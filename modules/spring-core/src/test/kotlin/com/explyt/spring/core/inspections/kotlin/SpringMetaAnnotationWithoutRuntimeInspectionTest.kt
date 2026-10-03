/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.inspections.SpringMetaAnnotationWithoutRuntimeInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

class SpringMetaAnnotationWithoutRuntimeInspectionTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springBootAutoConfigure_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringMetaAnnotationWithoutRuntimeInspection::class.java)
    }

    fun testExplicitRuntimeRetention() = assertHighlighting(
        """
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.RUNTIME)
        @MustBeDocumented
        @ConditionalOnProperty(prefix = "explyt.feature", name = ["enabled"], havingValue = "true")
        annotation class ConditionalOnFeature
        """
    )

    fun testDefaultRetentionIsRuntime() = assertHighlighting(
        """
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

        @Target(AnnotationTarget.CLASS)
        @ConditionalOnProperty(prefix = "explyt.feature", name = ["enabled"], havingValue = "true")
        annotation class ConditionalOnFeature
        """
    )

    fun testSourceRetention() = assertHighlighting(
        """
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.SOURCE)
        @ConditionalOnProperty(prefix = "explyt.feature", name = ["enabled"], havingValue = "true")
        annotation class <error descr="$RETENTION_PROBLEM">ConditionalOnFeature</error>
        """
    )

    fun testBinaryRetention() = assertHighlighting(
        """
        import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty

        @Target(AnnotationTarget.CLASS)
        @Retention(AnnotationRetention.BINARY)
        @ConditionalOnProperty(prefix = "explyt.feature", name = ["enabled"], havingValue = "true")
        annotation class <error descr="$RETENTION_PROBLEM">ConditionalOnFeature</error>
        """
    )

    fun testNestedMetaAnnotationWithRuntimeRetention() = assertHighlighting(
        """
        import org.springframework.context.annotation.Profile

        @Retention(AnnotationRetention.RUNTIME)
        @Profile("test")
        annotation class TestProfile

        @Retention(AnnotationRetention.RUNTIME)
        @TestProfile
        annotation class NestedTestProfile
        """
    )

    fun testNestedMetaAnnotationWithSourceRetention() = assertHighlighting(
        """
        import org.springframework.context.annotation.Profile

        @Profile("test")
        annotation class TestProfile

        @Retention(AnnotationRetention.SOURCE)
        @TestProfile
        annotation class <error descr="$RETENTION_PROBLEM">NestedTestProfile</error>
        """
    )

    fun testNotSpringMetaAnnotation() = assertHighlighting(
        """
        @Retention(AnnotationRetention.SOURCE)
        annotation class NotSpringAnnotation
        """
    )

    fun testCyclicMetaAnnotations() = assertHighlighting(
        """
        @Retention(AnnotationRetention.SOURCE)
        @SecondCyclic
        annotation class FirstCyclic

        @Retention(AnnotationRetention.SOURCE)
        @FirstCyclic
        annotation class SecondCyclic
        """
    )

    private fun assertHighlighting(@Language("kotlin") source: String) {
        myFixture.configureByText("MetaAnnotations.kt", source.trimIndent())
        myFixture.testHighlighting("MetaAnnotations.kt")
    }

    private companion object {
        val RETENTION_PROBLEM: String = SpringCoreBundle.message("explyt.spring.inspection.retention.incorrect")
    }
}
