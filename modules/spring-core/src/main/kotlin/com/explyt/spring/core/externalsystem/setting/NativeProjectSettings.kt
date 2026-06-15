/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.externalsystem.setting

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings

/*
* externalProjectPath containing qualified @SpringBootApplication class name
* */
class NativeProjectSettings : ExternalProjectSettings() {

    var runConfigurationName: String? = null
    var runConfigurationId: String? = null
    var qualifiedMainClassName: String? = null
    var runConfigurationType = RunConfigurationType.EXPLYT

    override fun clone(): NativeProjectSettings {
        val result = NativeProjectSettings()
        copyTo(result)
        result.runConfigurationName = runConfigurationName
        result.runConfigurationId = runConfigurationId
        result.runConfigurationType = runConfigurationType
        result.qualifiedMainClassName = qualifiedMainClassName
        return result
    }
}