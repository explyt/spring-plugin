package com.esprito.spring.core.completion.properties

import com.esprito.module.ExternalSystemModule
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import java.io.File
import java.io.FileFilter

class SpringMetadataProjectConfigurationPropertiesLoader(project: Project) : AbstractSpringMetadataConfigurationPropertiesLoader(project) {

    override fun loadProperties(module: Module): List<ConfigurationProperty> {
        // TODO: not load from resource directory META-INF
        //  additional-spring-configuration-metadata.json
        //  spring-configuration-metadata.json

        return emptyList()
    }

    override fun loadPropertyHints(module: Module): List<PropertyHint> {
        // TODO: not load from resource directory META-INF
        //  additional-spring-configuration-metadata.json
        //  spring-configuration-metadata.json

        return emptyList()
    }

    private fun findMetadataFiles(module: Module): Array<File> {
        //TODO load additional-spring-configuration-metadata.json from module resource META-ING directory
        val buildMetaInfDirectory = ExternalSystemModule.of(module).buildMetaInfDirectory ?: return emptyArray()
        return buildMetaInfDirectory.listFiles(FileFilter {
            CONFIGURATION_METADATA_FILE_NAME.equals(it.name, true)
                    || ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME.equals(it.name, true)
        }) ?: emptyArray()
    }
}