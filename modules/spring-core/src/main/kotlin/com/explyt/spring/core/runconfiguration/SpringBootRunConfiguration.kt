/*
 * Copyright © 2024 Explyt Ltd
 *
 * All rights reserved.
 *
 * This code and software are the property of Explyt Ltd and are protected by copyright and other intellectual property laws.
 *
 * You may use this code under the terms of the Explyt Source License Version 1.0 ("License"), if you accept its terms and conditions.
 *
 * By installing, downloading, accessing, using, or distributing this code, you agree to the terms and conditions of the License.
 * If you do not agree to such terms and conditions, you must cease using this code and immediately delete all copies of it.
 *
 * You may obtain a copy of the License at: https://github.com/explyt/spring-plugin/blob/main/EXPLYT-SOURCE-LICENSE.md
 *
 * Unauthorized use of this code constitutes a violation of intellectual property rights and may result in legal action.
 */

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.explyt.spring.core.runconfiguration.edit.SpringBootConfigurationEditor
import com.intellij.execution.Executor
import com.intellij.execution.application.ApplicationConfiguration
import com.intellij.execution.configurations.*
import com.intellij.execution.filters.TextConsoleBuilderFactory
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.project.Project

class SpringBootRunConfiguration(
    project: Project,
    factory: ConfigurationFactory
) : ApplicationConfiguration(
    "Explyt Spring Boot",
    project,
    factory
) {
    private val myOptions: SpringBootConfigurationOptions
        get() = options as SpringBootConfigurationOptions

    var springProfiles: String?
        get() {
            return myOptions.springProfiles
        }
        set(value) {
            myOptions.springProfiles = value
        }


    override fun getType(): ConfigurationType = SpringBootConfigurationType.getInstance()

    override fun getState(executor: Executor, env: ExecutionEnvironment): RunProfileState {
        val state = SpringBootRunConfigurationState(this, env)
        state.consoleBuilder = TextConsoleBuilderFactory.getInstance()
            .createBuilder(project, configurationModule.searchScope)
        return state
    }

    fun modifyJavaParams(params: JavaParameters) {
        with(myOptions) {
            if (!springProfiles.isNullOrEmpty()) {
                params.vmParametersList.add("-D$SPRING_PROFILES_ACTIVE=$springProfiles")
            }
        }
        //Activate JMX
        params.vmParametersList.add("-Dcom.sun.management.jmxremote")
        params.vmParametersList.add("-Dspring.jmx.enabled=true")
        params.vmParametersList.add("-Dspring.liveBeansView.mbeanDomain")
        params.vmParametersList.add("-Dspring.application.admin.enabled=true")
        params.vmParametersList.add("-Dmanagement.endpoints.jmx.exposure.include=*")
    }

    override fun getConfigurationEditor(): SettingsEditor<out RunConfiguration> {
        return SpringBootConfigurationEditor(this)
    }
}