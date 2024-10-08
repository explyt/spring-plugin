package com.esprito.spring.core.externalsystem.setting

import com.intellij.openapi.externalSystem.settings.ExternalProjectSettings

/*
* externalProjectPath containing qualified @SpringBootApplication class name
* */
class NativeProjectSettings : ExternalProjectSettings() {

    var runConfigurationName: String? = null

    override fun clone(): NativeProjectSettings {
        val result = NativeProjectSettings()
        copyTo(result)
        result.runConfigurationName = runConfigurationName
        return result
    }
}