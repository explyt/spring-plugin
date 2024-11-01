/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.inspections.SpringYamlInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.jetbrains.kotlin.test.TestMetadata

class SpringYamlInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(TestLibrary.springContext_6_0_7, TestLibrary.springBoot_3_1_1)

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringYamlInspection::class.java)
    }

    @TestMetadata("yaml")
    fun testYaml() = doTest(SpringYamlInspection())

    fun testDuplicateProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <error>barBaz</error>: some1
    <error>bar-Baz</error>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }

    fun testNoDuplicateProperties() {
        myFixture.configureByText(
            "application.yaml",
            """
foo:
    <warning descr="Cannot resolve key property 'foo.barBaz'"><warning descr="Should be kebab-case">barBaz</warning></warning>: some1
    <warning descr="Cannot resolve key property 'foo.bar-Baz1'"><warning descr="Should be kebab-case">bar-Baz1</warning></warning>: some1
            """.trimIndent()
        )
        myFixture.testHighlighting("application.yaml")
    }
}