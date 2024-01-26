package com.esprito.spring.core.completion.properties

import com.esprito.spring.core.SpringProperties.ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
import com.esprito.spring.core.SpringProperties.CONFIGURATION_METADATA_FILE_NAME
import com.esprito.spring.core.SpringProperties.PROPERTIES
import com.esprito.spring.core.tracker.ModificationTrackerManager
import com.esprito.util.CacheKeyStore
import com.intellij.java.library.JavaLibraryModificationTracker
import com.intellij.json.psi.JsonFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.impl.LibraryScopeCache
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.CommonProcessors

class SpringMetadataLibraryConfigurationPropertiesLoader(project: Project) :
    AbstractSpringMetadataConfigurationPropertiesLoader(project) {

    override fun loadProperties(module: Module): List<ConfigurationProperty> {
        val project = module.project
        val key = CacheKeyStore.getInstance(project).getKey<List<ConfigurationProperty>>(
            "SpringMetadataConfigurationPropertiesLoaderCache(${module.name})"
        )
        return CachedValuesManager
            .getManager(project)
            .getCachedValue(module, key, {
                val result = mutableMapOf<String, ConfigurationProperty>()
                findMetadataFiles(module).forEach {
                    collectConfigurationProperties(it.text, it.virtualFile.path, result)
                }
                CachedValueProvider.Result.create(
                    result.values.toList(),
                    JavaLibraryModificationTracker.getInstance(project)
                )
            }, false)
    }

    override fun loadPropertyHints(module: Module): List<PropertyHint> {
        val project = module.project
        val key = CacheKeyStore.getInstance(project).getKey<List<PropertyHint>>(
            "SpringMetadataConfigurationPropertyHintsLoaderCache(${module.name})"
        )
        return CachedValuesManager
            .getManager(project)
            .getCachedValue(module, key, {
                val result = findMetadataFiles(module).flatMap {
                    collectPropertyHints(it.text, it.virtualFile.path)
                }
                CachedValueProvider.Result.create(
                    result,
                    JavaLibraryModificationTracker.getInstance(project)
                )
            }, false)
    }

    override fun loadPropertyMetadataElements(module: Module): List<ElementHint> {
        val project = module.project
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                findMetadataFiles(module, true)
                    .filterIsInstance<JsonFile>()
                    .flatMap { collectElementMetadataName(it, PROPERTIES) },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    override fun loadMetadataElements(module: Module): List<ElementHint> {
        return emptyList()
    }

    private fun findMetadataFiles(module: Module, onlyAdditional: Boolean = false): List<PsiFile> {
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()

        val fileName = mutableSetOf(ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME)
        if (!onlyAdditional) {
            fileName.add(CONFIGURATION_METADATA_FILE_NAME)
        }

        FilenameIndex.processFilesByNames(
            fileName,
            true,
            LibraryScopeCache.getInstance(module.project).librariesOnlyScope,
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