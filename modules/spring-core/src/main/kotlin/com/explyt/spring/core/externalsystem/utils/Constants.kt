/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.utils

import com.intellij.openapi.externalSystem.model.ProjectSystemId
import java.util.*

object Constants {
    const val SPRING_BOOT_NATIVE_ID = "ExplytSpringBoot"
    const val DEBUG_SESSION_NAME = "-DebugSession-"

    //should be equals with SpringToolRunConfigurationConfigurable display name
    val SYSTEM_ID = ProjectSystemId(SPRING_BOOT_NATIVE_ID.uppercase(Locale.getDefault()), "Explyt Spring")
}