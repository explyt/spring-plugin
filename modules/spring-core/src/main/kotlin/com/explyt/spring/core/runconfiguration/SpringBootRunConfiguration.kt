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