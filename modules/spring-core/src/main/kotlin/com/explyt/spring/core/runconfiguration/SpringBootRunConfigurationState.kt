package com.explyt.spring.core.runconfiguration

import com.intellij.execution.application.ApplicationCommandLineState
import com.intellij.execution.configurations.JavaParameters
import com.intellij.execution.runners.ExecutionEnvironment

class SpringBootRunConfigurationState(
    configuration: SpringBootRunConfiguration,
    environment: ExecutionEnvironment
) : ApplicationCommandLineState<SpringBootRunConfiguration>(
    configuration,
    environment
) {
    override fun createJavaParameters(): JavaParameters {
        val params = super.createJavaParameters()
        myConfiguration.modifyJavaParams(params)
        return params
    }

    override fun isProvidedScopeIncluded(): Boolean = myConfiguration.isProvidedScopeIncluded
}