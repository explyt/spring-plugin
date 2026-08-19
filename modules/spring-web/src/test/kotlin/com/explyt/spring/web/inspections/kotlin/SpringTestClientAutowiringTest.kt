/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.kotlin

import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.test.ExplytInspectionKotlinTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

/**
 * `MockMvc` is contributed by Spring Boot's *test* auto-configuration (`@AutoConfigureMockMvc`, or implicitly by
 * slice annotations such as `@WebMvcTest`), which is not part of the regular auto-configuration model. Autowiring
 * it must not be reported as a missing bean.
 */
class SpringTestClientAutowiringTest : ExplytInspectionKotlinTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springWeb_6_0_7,
        TestLibrary.springBootTestAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    fun testMockMvcFieldInjectionNotReported() {
        @Language("kotlin") val code = """
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
class MockMvcFieldTest {
    @Autowired
    lateinit var mockMvc: MockMvc
}
            """
        myFixture.configureByText("MockMvcFieldTest.kt", code.trimIndent())
        myFixture.testHighlighting("MockMvcFieldTest.kt")
    }

    fun testMockMvcConstructorInjectionNotReported() {
        @Language("kotlin") val code = """
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.web.servlet.MockMvc

@SpringBootTest
@AutoConfigureMockMvc
class MockMvcConstructorTest(
    @Autowired private val mockMvc: MockMvc,
)
            """
        myFixture.configureByText("MockMvcConstructorTest.kt", code.trimIndent())
        // Errors only: an undecorated `@Autowired` constructor parameter also raises the Kotlin
        // ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD warning (KT-73255), which is unrelated here.
        myFixture.testHighlighting(false, false, false, "MockMvcConstructorTest.kt")
    }

    fun testWebMvcTestSliceAnnotationNotReported() {
        @Language("kotlin") val code = """
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.test.web.servlet.MockMvc

@WebMvcTest
class MockMvcSliceTest {
    @Autowired
    lateinit var mockMvc: MockMvc
}
            """
        myFixture.configureByText("MockMvcSliceTest.kt", code.trimIndent())
        myFixture.testHighlighting("MockMvcSliceTest.kt")
    }
}
