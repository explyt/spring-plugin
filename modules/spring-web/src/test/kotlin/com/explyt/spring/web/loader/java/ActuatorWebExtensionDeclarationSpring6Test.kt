/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.loader.java

import com.explyt.spring.test.TestLibrary

class ActuatorWebExtensionDeclarationSpring6Test : ActuatorWebExtensionDeclarationTest() {

    override val libraries: Array<TestLibrary> =
        arrayOf(TestLibrary("org.springframework.boot:spring-boot-actuator-autoconfigure:3.5.8"))

    override val conventionOverridesMetaValues: Boolean = true
}
