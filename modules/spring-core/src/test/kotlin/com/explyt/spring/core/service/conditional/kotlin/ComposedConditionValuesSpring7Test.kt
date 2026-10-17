/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.ComposedConditionValuesTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.explyt.spring.test.TestLibrary

class ComposedConditionValuesSpring7Test : ComposedConditionValuesTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary("org.springframework:spring-context:7.0.9"),
        TestLibrary.springBootAutoConfigure_4_1_0,
        TestLibrary("org.springframework:spring-web:7.0.9"),
        TestLibrary.jakartaServlet_6_1_0,
    )
    override val springMajor: Int = 7
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}
