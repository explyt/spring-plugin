package com.esprito.spring.core.profile

import com.esprito.spring.core.SpringProperties.SPRING_PROFILES_ACTIVE
import com.esprito.spring.core.tracker.ModificationTrackerManager
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

    override fun searchProfiles(module: Module): List<String> {
        val project = module.project

        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                findPomXml(module).flatMap { getProfiles(it) },
                ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
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