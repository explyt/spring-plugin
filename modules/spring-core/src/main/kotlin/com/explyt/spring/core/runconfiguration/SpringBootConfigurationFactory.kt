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

import com.explyt.spring.core.util.SpringCoreUtil
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
