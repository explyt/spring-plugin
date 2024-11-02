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

package com.explyt.spring.core.profile

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.explyt.spring.core.tracker.XmlModificationTracker
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.xml.XmlFile
import com.intellij.util.CommonProcessors

class MavenProfileSearcher(project: Project) : ProfileSearcher {

    private val psiManager = PsiManager.getInstance(project)

    override fun searchActiveProfiles(module: Module): List<String> {
        return emptyList()
    }

    override fun searchProfiles(module: Module): List<String> {
        val project = module.project

        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                findPomXml(module).flatMap { getProfiles(it) },
                XmlModificationTracker.getInstance(project)
            )
        }
    }

    private fun getProfiles(psiFile: PsiFile): List<String> {
        if (psiFile !is XmlFile) return listOf()

        val rootTag = psiFile.document?.rootTag ?: return listOf()
        if (rootTag.name != "project") return listOf()

        val profilesFromMavenPlugin = rootTag
            .findSubTags("build").asSequence()
            .flatMap { it.findSubTags("plugins").asSequence() }
            .flatMap { it.findSubTags("plugin").asSequence() }
            .filter { it.findFirstSubTag("groupId")?.value?.text?.trim() == "org.springframework.boot" }
            .filter { it.findFirstSubTag("artifactId")?.value?.text?.trim() == "spring-boot-maven-plugin" }
            .flatMap { it.findSubTags("configuration").asSequence() }
            .flatMap { it.findSubTags("profiles").asSequence() }
            .flatMap { it.findSubTags("profile").asSequence() }
            .flatMap { it.value.text.split(',').asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()

        val profilesFromMavenProfiles = rootTag
            .findSubTags("profiles").asSequence()
            .flatMap { it.findSubTags("profile").asSequence() }
            .flatMap { it.findSubTags("properties").asSequence() }
            .flatMap { it.findSubTags(SPRING_PROFILES_ACTIVE).asSequence() }
            .flatMap { it.value.text.split(',').asSequence() }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()


        return profilesFromMavenPlugin + profilesFromMavenProfiles

    }

    private fun findPomXml(module: Module): List<PsiFile> {
        val basePath = module.project.basePath ?: return listOf()
        val collectProcessor = CommonProcessors.CollectProcessor<VirtualFile>()

        FilenameIndex.processFilesByNames(
            setOf("pom.xml"),
            true,
            GlobalSearchScope.projectScope(module.project),
            null,
            collectProcessor
        )

        return collectProcessor.results.filter {
            it.parent.canonicalPath == basePath
        }.mapNotNull {
            psiManager.findFile(it)
        }

    }

}