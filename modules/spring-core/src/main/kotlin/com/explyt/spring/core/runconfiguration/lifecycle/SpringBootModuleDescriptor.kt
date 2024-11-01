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

package com.explyt.spring.core.runconfiguration.lifecycle

import com.explyt.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.explyt.spring.core.util.SpringBootUtil
import com.intellij.openapi.module.Module

private const val DEFAULT_DESCRIPTOR_NAME = "org.springframework.boot:type=Admin,name=SpringApplication"

data class SpringBootModuleDescriptor(
    val applicationAdminJmxName: String,
    val version: SpringBootUtil.SpringBootVersion? = null
) {
    companion object {
        val DEFAULT_DESCRIPTOR = SpringBootModuleDescriptor(
            DEFAULT_DESCRIPTOR_NAME
        )

        fun of(module: Module?): SpringBootModuleDescriptor {
            return SpringBootModuleDescriptor(
                getApplicationAdminJmxName(module),
                module?.let { SpringBootUtil.getSpringBootVersion(it) }
            )
        }


        private fun getApplicationAdminJmxName(module: Module?): String {
            return module?.let {
                DefinedConfigurationPropertiesSearch.getInstance(it.project)
                    .findProperties(it, "spring.application.admin.jmx-name").asSequence()
                    .mapNotNull { property -> property.value }
                    .firstOrNull()
            } ?: DEFAULT_DESCRIPTOR_NAME
        }

    }
}
