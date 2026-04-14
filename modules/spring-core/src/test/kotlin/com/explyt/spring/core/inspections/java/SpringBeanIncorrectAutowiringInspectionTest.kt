/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.inspections.java

import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.inspections.SpringBeanIncorrectAutowiringInspection
import com.explyt.spring.test.ExplytInspectionJavaTestCase
import com.explyt.spring.test.TestLibrary
import org.intellij.lang.annotations.Language
import org.jetbrains.kotlin.test.TestMetadata

class SpringBeanIncorrectAutowiringInspectionTest : ExplytInspectionJavaTestCase() {

    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springContext_6_0_7,
        TestLibrary.springTest_6_0_7,
        TestLibrary.springBootTestAutoConfigure_3_1_1
    )

    override fun setUp() {
        super.setUp()
        myFixture.enableInspections(SpringBeanIncorrectAutowiringInspection::class.java)
    }

    @TestMetadata("autowired")
    fun testAutowired() = doTest(SpringBeanIncorrectAutowiringInspection())

    fun testResourceLoader() {
        @Language("java") val code = """

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.core.io.ResourceLoader;
import org.springframework.context.support.AbstractApplicationContext;

@${SpringCoreClasses.COMPONENT}
public class DemoApplication {
    @Autowired
    ResourceLoader resourceLoader;
    @Autowired
    ApplicationContext context;
    @Autowired
    AbstractApplicationContext abstractContext;
}
            """
        myFixture.configureByText("DemoApplication.java", code.trimIndent())
        myFixture.testHighlighting("DemoApplication.java")
    }
}