/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary

class SpringProfileAnnotationInspectionTest : ExplytInspectionJavaTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(com.explyt.spring.core.inspections.SpringProfileAnnotationInspection::class.java)
    }

    fun testValidProfileExpression() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}("profile1 & (profile2 | profile3)")
            public class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testValidProfileArraysExpression() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}({"profile1","profile3"})
            public class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testEmptyProfileExpression() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}(<error descr="Must not be empty">""</error>)
            public class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInvalidProfileExpression() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}("profile1 | profile2<error descr="ProfilesTokenType.| expected, got '&'">&</error> & profile3")
            public class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

    fun testInvalidProfileArraysExpression() {
        myFixture.configureByText(
            "SpringBean.java",
            """
            @${SpringCoreClasses.COMPONENT}
            @${SpringCoreClasses.PROFILE}({
                "profile1 | profile2<error descr="ProfilesTokenType.| expected, got '&'">&</error>",
                "abc & abd<error descr="ProfilesTokenType.& expected, got '|'">|</error> abe"
            })
            public class SpringBean {}
            """.trimIndent()
        )
        myFixture.testHighlighting("SpringBean.java")
    }

}
