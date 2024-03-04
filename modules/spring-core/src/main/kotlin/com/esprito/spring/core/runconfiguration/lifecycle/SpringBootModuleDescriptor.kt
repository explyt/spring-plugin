package com.esprito.spring.core.runconfiguration.lifecycle

import com.esprito.spring.core.completion.properties.DefinedConfigurationPropertiesSearch
import com.esprito.spring.core.util.SpringBootUtil
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
