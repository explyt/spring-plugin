/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.runconfiguration

import com.intellij.execution.application.JvmMainMethodRunConfigurationOptions
import com.intellij.util.xmlb.annotations.OptionTag

class SpringBootConfigurationOptions : JvmMainMethodRunConfigurationOptions() {
    @get:OptionTag("SPRING_PROFILES")
    var springProfiles: String? by string()
}