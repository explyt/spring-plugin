/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.service.conditional.kotlin

import com.explyt.spring.core.service.conditional.Boot4ReactiveWebApplicationTypeTestCase
import com.explyt.spring.core.service.conditional.Boot4ReactiveWithoutBootWebFluxWebApplicationTypeTestCase
import com.explyt.spring.core.service.conditional.ConditionFixtures
import com.explyt.spring.core.service.conditional.ReactiveOnlyWebApplicationTypeTestCase
import com.explyt.spring.core.service.conditional.ServletAndWebFluxWebApplicationTypeTestCase
import com.explyt.spring.core.service.conditional.ServletOnlyWebApplicationTypeTestCase
import com.explyt.spring.core.service.conditional.SpringWebOnlyWebApplicationTypeTestCase

class ServletAndWebFluxWebApplicationTypeTest : ServletAndWebFluxWebApplicationTypeTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}

class SpringWebOnlyWebApplicationTypeTest : SpringWebOnlyWebApplicationTypeTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}

class ReactiveOnlyWebApplicationTypeTest : ReactiveOnlyWebApplicationTypeTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}

class ServletOnlyWebApplicationTypeTest : ServletOnlyWebApplicationTypeTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}

class Boot4ReactiveWithoutBootWebFluxWebApplicationTypeTest : Boot4ReactiveWithoutBootWebFluxWebApplicationTypeTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}

class Boot4ReactiveWebApplicationTypeTest : Boot4ReactiveWebApplicationTypeTestCase() {
    override val fixtures: ConditionFixtures by lazy { KotlinConditionFixtures(myFixture) }
}
