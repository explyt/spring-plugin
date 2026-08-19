/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.inspections.java

import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language

/**
 * `MockMvc` is contributed by Spring Boot's *test* auto-configuration (`@AutoConfigureMockMvc`, or implicitly by
 * slice annotations such as `@WebMvcTest`), which is not part of the regular auto-configuration model. Autowiring
 * it must not be reported as a missing bean.
 */
class SpringTestClientAutowiringTest : ExplytInspectionJavaTestCase() {

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
        @Language("java") val code = """
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class MockMvcFieldTest {
    @Autowired
    MockMvc mockMvc;
}
            """
        myFixture.configureByText("MockMvcFieldTest.java", code.trimIndent())
        myFixture.testHighlighting("MockMvcFieldTest.java")
    }

    fun testMockMvcConstructorInjectionNotReported() {
        @Language("java") val code = """
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
public class MockMvcConstructorTest {
    private final MockMvc mockMvc;

    @Autowired
    public MockMvcConstructorTest(MockMvc mockMvc) {
        this.mockMvc = mockMvc;
    }
}
            """
        myFixture.configureByText("MockMvcConstructorTest.java", code.trimIndent())
        myFixture.testHighlighting("MockMvcConstructorTest.java")
    }

    fun testWebMvcTestSliceAnnotationNotReported() {
        @Language("java") val code = """
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest
public class MockMvcSliceTest {
    @Autowired
    MockMvc mockMvc;
}
            """
        myFixture.configureByText("MockMvcSliceTest.java", code.trimIndent())
        myFixture.testHighlighting("MockMvcSliceTest.java")
    }
}
