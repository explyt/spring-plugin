package com.esprito.spring.core.completion.properties

import com.esprito.module.ExternalSystemModule
import com.esprito.util.CacheKeyStore
import com.intellij.codeInsight.JavaLibraryModificationTracker
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
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
            "SpringMetadataConfigurationPropertiesLoaderCache(${module.name})")
        return CachedValuesManager
            .getManager(project)
            .getCachedValue(module, key, {
                val result = findMetadataFiles(module).flatMap {
                    collectConfigurationProperties(it.text, it.virtualFile.path)
                }
                CachedValueProvider.Result.create(result, JavaLibraryModificationTracker.getInstance(project))
            }, false)
    }

    private fun findMetadataFiles(module: Module): List<PsiFile> {
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()

        FilenameIndex.processFilesByNames(
            setOf(
                CONFIGURATION_METADATA_FILE_NAME,
                ADDITIONAL_CONFIGURATION_METADATA_FILE_NAME
            ),
            true,
            ExternalSystemModule.of(module).librariesSearchScope,
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