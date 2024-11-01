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