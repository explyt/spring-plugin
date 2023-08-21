package com.esprito.spring.core.runconfiguration

import com.esprito.spring.core.util.SpringCoreUtil
import com.intellij.execution.configurations.ConfigurationFactory
import com.intellij.openapi.components.BaseState
import com.intellij.openapi.project.Project

object SpringBootConfigurationFactory :
    ConfigurationFactory(
        SpringBootConfigurationType.getInstance()
    ) {
    override fun createTemplateConfiguration(project: Project): SpringBootRunConfiguration =
        SpringBootRunConfiguration(project, this)

    override fun isApplicable(project: Project): Boolean = SpringCoreUtil.isSpringBootProject(project)

    override fun getId(): String = "Spring Boot"

    override fun isEditableInDumbMode(): Boolean = true

    override fun getOptionsClass(): Class<out BaseState> = SpringBootConfigurationOptions::class.java

}
