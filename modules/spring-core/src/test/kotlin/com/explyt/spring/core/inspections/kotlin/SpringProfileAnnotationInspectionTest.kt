/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.kotlin

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringProfileAnnotationInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import com.intellij.psi.JavaPsiFacade

class SpringProfileAnnotationInspectionTest : ExplytInspectionKotlinTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringProfileAnnotationInspection::class.java)
        assertNotNull(
            "@Profile must resolve to Spring's annotation",
            JavaPsiFacade.getInstance(project).findClass(SpringCoreClasses.PROFILE, module.moduleWithLibrariesScope)
        )
    }

    fun testEmptyProfileArrayLiteral() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}(value = <error descr="@Profile must name at least one profile">[]</error>)
            class SpringBean
            """
        )
    }

    fun testEmptyArrayOfCall() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}(value = <error descr="@Profile must name at least one profile">arrayOf()</error>)
            class SpringBean
            """
        )
    }

    fun testSpreadOfAnEmptyArray() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}(*<error descr="@Profile must name at least one profile">arrayOf()</error>)
            class SpringBean
            """
        )
    }

    fun testProfileWithoutArguments() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            <error descr="@Profile must name at least one profile">@${SpringCoreClasses.PROFILE}()</error>
            class SpringBean
            """
        )
    }

    fun testEmptyProfileArrayOnBeanFunction() {
        assertHighlighting(
            """
            @${SpringCoreClasses.CONFIGURATION}
            class SpringConfig {
                @${SpringCoreClasses.BEAN}
                @${SpringCoreClasses.PROFILE}(value = <error descr="@Profile must name at least one profile">[]</error>)
                fun name(): String = ""
            }
            """
        )
    }

    fun testProfileWithOneProfileIsNotReported() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}("dev")
            class SpringBean
            """
        )
    }

    fun testProfileWithAProfileAndANegationIsNotReported() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}("dev", "!prod")
            class SpringBean
            """
        )
    }

    fun testProfileArrayLiteralWithAProfileIsNotReported() {
        assertHighlighting(
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}(value = ["dev", "!prod"])
            class SpringBean
            """
        )
    }

    private fun assertHighlighting(source: String) {
        myFixture.configureByText("SpringBean.kt", source.trimIndent())
        myFixture.testHighlighting("SpringBean.kt")
    }
}
