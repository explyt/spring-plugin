package com.esprito.spring.core.completion.properties

import com.esprito.module.ExternalSystemModule
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.io.FileUtil
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import java.io.File
import java.io.FileFilter

class SpringMetadataProjectConfigurationPropertiesLoader(project: Project)
    : AbstractSpringMetadataConfigurationPropertiesLoader(project) {

    companion object {
        private val CACHE_KEY =
            Key.create<CachedValue<List<ConfigurationProperty>>>("SpringMetadataProjectConfigurationPropertiesLoader")
    }

    override fun loadProperties(module: Module): List<ConfigurationProperty> {
        return CachedValuesManager
            .getManager(module.project)
            .getCachedValue(module, CACHE_KEY, {
                val result = findMetadataFiles(module).flatMap {
                    collectConfigurationProperties(FileUtil.loadFile(it), it.absolutePath)
                }
                //TODO check ProjectRootModificationTracker
                CachedValueProvider.Result.create(result, ProjectRootModificationTracker.getInstance(module.project))
            }, false)
    }

    private fun findMetadataFiles(module: Module): Array<File> {
        //TODO load additional-spring-configuration-metadata.json from module resource META-ING directory
        val buildMetaInfDirectory = ExternalSystemModule.of(module).buildMetaInfDirectory ?: return emptyArray()
        return buildMetaInfDirectory.listFiles(FileFilter {
            CONFIGURATION_METADATA_FILE_NAME.equals(it.name, true) || ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME.equals(it.name, true)
        }) ?: emptyArray()
    }
}