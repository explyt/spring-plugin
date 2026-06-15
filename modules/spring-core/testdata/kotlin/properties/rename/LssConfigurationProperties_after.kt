/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "lss")
data class LssConfigurationProperties(
    var modeForStudioNew: String = "",
    var tokenForStudio: String = "",
)