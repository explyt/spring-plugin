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