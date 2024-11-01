package com.explyt.spring.core.completion.properties

import com.intellij.lang.properties.IProperty
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.extensions.ProjectExtensionPointName
import com.intellij.openapi.module.Module
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.util.CommonProcessors

interface ConfigurationFactoriesNamesLoader {

    companion object {
        val EP_NAME = ProjectExtensionPointName<ConfigurationFactoriesNamesLoader>(
            "com.explyt.spring.core.configurationFactoriesNamesLoader"
        )
    }

    fun loadFactories(module: Module): Set<String>

    fun findMetadataProperties(module: Module): List<IProperty> {
        val scope = GlobalSearchScope.moduleScope(module)
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()

        val psiManager = PsiManager.getInstance(module.project)

        FilenameIndex.processFilesByNames(
            setOf(getFileName()),
            true,
            scope,
            null,
            collectProcessor
        )

        return collectProcessor.results.asSequence()
            .filter { getFileFilter(it) }
            .mapNotNull { psiManager.findFile(it) }
            .filterIsInstance(PropertiesFile::class.java)
            .flatMap { it.properties }
            .toList()
    }

    fun getFileName(): String

    fun getFileFilter(file: VirtualFile): Boolean
}