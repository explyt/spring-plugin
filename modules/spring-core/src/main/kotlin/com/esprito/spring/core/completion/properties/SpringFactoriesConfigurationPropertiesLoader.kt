package com.esprito.spring.core.completion.properties

import com.esprito.util.CacheKeyStore
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.ProjectScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.CommonProcessors

class SpringFactoriesConfigurationPropertiesLoader(project: Project) :
    AbstractSpringMetadataConfigurationPropertiesLoader(project) {

    override fun loadProperties(module: Module): List<ConfigurationProperty> {
        val key = CacheKeyStore.getInstance(module.project).getKey<List<ConfigurationProperty>>(
            "SpringFactoriesConfigurationPropertiesLoader"
        )
        return CachedValuesManager
            .getManager(module.project)
            .getCachedValue(module, key, {
                val result = mutableMapOf<String, ConfigurationProperty>()
                findMetadataFiles(module).forEach {
                    collectConfigurationProperties(it, result)
                }

                CachedValueProvider.Result.create(
                    result.values.toList(),
                    ProjectRootModificationTracker.getInstance(module.project)
                )
            }, false)
    }

    private fun collectConfigurationProperties(
        psiFile: PsiFile,
        configurationProperties: MutableMap<String, ConfigurationProperty>
    ) {
        if (psiFile !is PropertiesFile) return

        psiFile.properties.forEach {
            configurationProperties[it.name] = ConfigurationProperty(
                name = it.name,
                type = null,
                sourceType = null,
                description = null,
                defaultValue = it.unescapedValue
            )
        }
    }

    override fun loadPropertyHints(module: Module) = emptyList<PropertyHint>()

    private fun findMetadataFiles(module: Module): List<PsiFile> {
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()

        FilenameIndex.processFilesByNames(
            setOf(SPRING_FACTORIES_FILE_NAME),
            true,
            ProjectScope.getProjectScope(module.project),
            null,
            collectProcessor
        )

        return collectProcessor.results.filter {
            it.parent?.name == "META-INF"
        }.mapNotNull {
            psiManager.findFile(it)
        }.sortedBy { it.name }
    }

}