/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.java

import com.explyt.spring.core.service.conditional.BeanConditionVerdictTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures

class BeanConditionVerdictTest : BeanConditionVerdictTestCase() {
    override val fixtures: ConditionFixtures by lazy { JavaConditionFixtures(myFixture) }
}
