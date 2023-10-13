package com.esprito.spring.core.completion.properties

import com.esprito.util.CacheKeyStore
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.CommonProcessors

class AutoConfigurationImportsPropertiesLoader :
    ConfigurationFactoriesNamesLoader {

    override fun loadFactories(module: Module): Set<String> {
        val key = CacheKeyStore.getInstance(module.project).getKey<Set<String>>(
            "AutoConfigurationImportsPropertiesLoader"
        )
        return CachedValuesManager.getManager(module.project)
            .getCachedValue(module, key, {
                CachedValueProvider.Result.create(
                    findMetadataFiles(module)
                        .asSequence()
                        .flatMap { getProperties(it).asSequence() }
                        .toSet(),
                    ProjectRootModificationTracker.getInstance(module.project)
                )
            }, false)
    }


    private fun getProperties(file: PsiFile): Set<String> {
        if (file !is PropertiesFile) return setOf()

        return file
            .properties
            .asSequence()
            .mapNotNull { it.unescapedKey }
            .flatMap { it.split(Regexes.whitespace) }
            .filter { it.isNotBlank() }
            .toSet()
    }

    private fun findMetadataFiles(module: Module): List<PsiFile> {
        val scope = GlobalSearchScope.moduleWithDependenciesAndLibrariesScope(module)
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()
        val psiManager = PsiManager.getInstance(module.project)

        FilenameIndex.processFilesByNames(
            setOf(AUTOCONFIGURATION_IMPORTS),
            true,
            scope,
            null,
            collectProcessor
        )

        return collectProcessor.results.asSequence()
            .filter {
                it.parent?.name == "spring" && it.parent?.parent?.name == "META-INF"
            }.mapNotNull {
                psiManager.findFile(it)
            }.toList()

    }

    private object Regexes {
        val whitespace = Regex("\\s")
    }

    private companion object {
        private const val AUTOCONFIGURATION_IMPORTS = "org.springframework.boot.autoconfigure.AutoConfiguration.imports"
    }

}