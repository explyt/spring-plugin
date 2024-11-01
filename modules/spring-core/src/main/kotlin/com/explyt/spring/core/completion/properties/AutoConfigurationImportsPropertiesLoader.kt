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

import com.explyt.spring.core.SpringProperties.AUTOCONFIGURATION_IMPORTS
import com.explyt.spring.core.SpringProperties.META_INF
import com.explyt.spring.core.SpringProperties.SPRING
import com.explyt.util.CacheKeyStore
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

class AutoConfigurationImportsPropertiesLoader : ConfigurationFactoriesNamesLoader {

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
            .filter { it.parent?.name == SPRING && it.parent?.parent?.name == META_INF }
            .mapNotNull { psiManager.findFile(it) }
            .toList()
    }

    override fun getFileName() = AUTOCONFIGURATION_IMPORTS

    override fun getFileFilter(file: VirtualFile): Boolean {
        return file.parent?.name == SPRING && file.parent?.parent?.name == META_INF
    }

    private object Regexes {
        val whitespace = Regex("\\s")
    }
}