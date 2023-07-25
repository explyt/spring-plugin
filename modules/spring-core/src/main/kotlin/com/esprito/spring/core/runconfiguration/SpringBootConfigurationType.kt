package com.esprito.spring.core.runconfiguration

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringIcons
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.execution.configurations.ConfigurationType
import com.intellij.execution.configurations.ConfigurationTypeUtil.findConfigurationType
import com.intellij.icons.AllIcons
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