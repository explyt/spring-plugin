/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.BeanConditionParityTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures

class BeanConditionParityTest : BeanConditionParityTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}
