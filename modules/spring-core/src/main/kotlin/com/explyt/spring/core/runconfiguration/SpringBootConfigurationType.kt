package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import javax.swing.Icon

class SpringBootConfigurationType : ConfigurationType {
    override fun getDisplayName(): String = SpringCoreBundle.message("run.configuration.spring.boot.display.name")

    override fun getConfigurationTypeDescription(): String =
        SpringCoreBundle.message("run.configuration.spring.boot.description")

    override fun getIcon(): Icon = SpringIcons.Spring

    override fun getId(): String = "Spring Boot"

    override fun getConfigurationFactories(): Array<ConfigurationFactory> = arrayOf(SpringBootConfigurationFactory)

    companion object {
        fun getInstance(): SpringBootConfigurationType {
            return findConfigurationType(SpringBootConfigurationType::class.java)
        }
    }
}