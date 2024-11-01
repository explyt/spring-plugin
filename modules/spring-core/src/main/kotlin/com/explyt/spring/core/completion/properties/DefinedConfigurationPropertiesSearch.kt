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

package com.explyt.spring.core.completion.properties

import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.PropertyUtil
import com.explyt.spring.core.util.SpringCoreUtil
import com.intellij.lang.properties.PropertiesLanguage
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.fileTypes.LanguageFileType
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.SmartPointerManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.CommonProcessors
import com.intellij.util.Processor
import org.jetbrains.yaml.YAMLLanguage
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.impl.YAMLPlainTextImpl

@Service(Service.Level.PROJECT)
class DefinedConfigurationPropertiesSearch(val project: Project) {

    companion object {
        val fileMask = Regex("application(-.+)?\\.(properties|yml|yaml)")
        fun getInstance(project: Project): DefinedConfigurationPropertiesSearch = project.service()
    }

    private val psiManager = PsiManager.getInstance(project)

    fun searchPropertyFiles(module: Module): List<PsiFile> {
        val project = module.project

        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                findPropertyFiles(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun findProperties(module: Module, key: String): List<DefinedConfigurationProperty> {
        return getAllProperties(module).filter { it.key == key }
    }

    fun getAllProperties(module: Module): List<DefinedConfigurationProperty> {
        return loadPropertySources(module).flatMap { it.properties }
    }

    fun getAllPlaceholders(module: Module): List<String> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                doGetPlaceholders(module),
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
            )
        }
    }

    fun getPropertiesCommonKeyMap(module: Module): Map<String, List<DefinedConfigurationProperty>> {
        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                toCommonKeyMap(module), ModificationTrackerManager.getInstance(project).getPropertyTracker()
            )
        }
    }

    private fun doGetPlaceholders(module: Module): List<String> {
        return getAllProperties(module).asSequence()
            .mapNotNull { it.value }
            .flatMap {
                PropertyUtil.getPlaceholders(it) { placeholder, _ -> placeholder }
            }
            .toList()
    }

    private fun toCommonKeyMap(module: Module): Map<String, List<DefinedConfigurationProperty>> {
        return getAllProperties(module).groupBy { PropertyUtil.toCommonPropertyForm(it.key) }
    }

    private fun loadPropertySources(module: Module): Set<PropertySource> {
        val propertiesFileType = PropertiesLanguage.INSTANCE.associatedFileType ?: return emptySet()
        val yamlFileType = YAMLLanguage.INSTANCE.associatedFileType ?: return emptySet()

        val sources = mutableSetOf<PropertySource>()
        collectPropertySources(propertiesFileType, sources, module.moduleWithDependentsScope)
        collectPropertySources(yamlFileType, sources, module.moduleWithDependentsScope)
        return sources
    }

    private fun collectPropertySources(
        fileType: LanguageFileType,
        sources: MutableSet<PropertySource>,
        scope: GlobalSearchScope
    ) {
        FileTypeIndex.processFiles(
            fileType,
            Processor {
                val foundPsiFile = psiManager.findFile(it)
                if (foundPsiFile != null && SpringCoreUtil.isConfigurationPropertyFile(foundPsiFile)) {
                    if (foundPsiFile is PropertiesFile) {
                        sources.add(PropertiesPropertySource(foundPsiFile))
                    } else if (foundPsiFile is YAMLFile) {
                        sources.add(YamlPropertySource(foundPsiFile))
                    }
                }
                return@Processor true
            },
            scope
        )
    }

    private fun findPropertyFiles(module: Module): List<PsiFile> {
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()

        val propertyFileNames = FilenameIndex.getAllFilenames(module.project).asSequence()
            .filter { fileMask.matches(it) }
            .toSet()

        FilenameIndex.processFilesByNames(
            propertyFileNames,
            true,
            GlobalSearchScope.projectScope(module.project),
            null,
            collectProcessor
        )

        return collectProcessor.results.filter {
            it.parent?.name == "resources"
        }.mapNotNull {
            psiManager.findFile(it)
        }.sortedBy { it.name }
    }
}

interface PropertySource {
    val properties: List<DefinedConfigurationProperty>
}

class PropertiesPropertySource(propertiesFile: PropertiesFile) : FilePropertySource(propertiesFile.containingFile) {

    override val properties: List<DefinedConfigurationProperty>
        get() {
            val propertiesFile = file as? PropertiesFile ?: return emptyList()
            return propertiesFile.properties.map {
                PropertyDefinedConfigurationProperty(it, propertiesFile.name)
            }
        }
}

class YamlPropertySource(yamlFile: YAMLFile) : FilePropertySource(yamlFile) {

    override val properties: List<DefinedConfigurationProperty>
        get() {
            val yamlFile = file as? YAMLFile ?: return emptyList()
            return yamlFile.documents.flatMap { document ->
                val result = mutableListOf<DefinedConfigurationProperty>()
                PsiTreeUtil.processElements(document, YAMLKeyValue::class.java) { keyValue ->
                    if (keyValue.value is YAMLPlainTextImpl) {
                        result.add(YamlDefinedConfigurationProperty(keyValue, yamlFile.name))
                    }
                    true
                }
                result
            }
        }
}

abstract class FilePropertySource(file: PsiFile) : PropertySource {

    private val filePointer = SmartPointerManager.createPointer<PsiFile>(file)

    val file: PsiFile?
        get() = filePointer.element

}

