/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.setting

import com.intellij.openapi.externalSystem.model.settings.ExternalSystemExecutionSettings
import com.intellij.openapi.project.Project

class NativeExecutionSettings(val project: Project) : ExternalSystemExecutionSettings() {
    var externalProjectMainFilePath: String? = null
    var runConfigurationName: String? = null
    var qualifiedMainClassName: String? = null
    var messageMappingExist: Boolean = false
    var aspectExist: Boolean = false
    var beanSearch: Boolean = false
    var runConfigurationType = RunConfigurationType.EXPLYT
}

enum class RunConfigurationType {
    EXPLYT, KOTLIN, APPLICATION
}