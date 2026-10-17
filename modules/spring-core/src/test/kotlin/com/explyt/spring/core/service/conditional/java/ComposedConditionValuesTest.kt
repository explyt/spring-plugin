/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.ComposedConditionValuesTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.explyt.spring.test.TestLibrary

class ComposedConditionValuesTest : ComposedConditionValuesTestCase() {
    override val libraries: Array<TestLibrary> = arrayOf(
        TestLibrary.springBootAutoConfigure_3_1_1,
        TestLibrary.springWeb_6_0_7,
    )
    override val springMajor: Int = 6
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }
}
