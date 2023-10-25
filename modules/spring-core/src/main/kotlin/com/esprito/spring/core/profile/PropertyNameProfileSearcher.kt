package com.esprito.spring.core.profile

import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.uast.UastModificationTracker
import com.intellij.util.CommonProcessors
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLFile

class PropertyNameProfileSearcher(project: Project) : ProfileSearcher {

    private val psiManager = PsiManager.getInstance(project)

    override fun searchProfiles(module: Module): List<String> {
        val project = module.project

        return CachedValuesManager.getManager(project).getCachedValue(module) {
            CachedValueProvider.Result(
                findPropertyFiles(module)
                    .flatMap { getProfiles(it) }
                    .plus("default"),
                UastModificationTracker.getInstance(project)
            )
        }

    }

    private fun getProfiles(psiFile: PsiFile): List<String> {
        val filename = psiFile.name
        if (filename == "application.properties") {
            return getProfilesFromProperties(psiFile)
        }
        if (setOf("application.yaml", "application.yml").contains(filename)) {
            return getProfilesFromYaml(psiFile)
        }
        return listOfNotNull(
            fileMask.find(filename)
                ?.groups
                ?.get(1)
                ?.value
                ?.substring(1)
        )
    }

    private fun getProfilesFromYaml(psiFile: PsiFile): List<String> {
        if (psiFile !is YAMLFile) return listOf()

        return YAMLUtil.getQualifiedKeyInFile(psiFile, "spring", "profiles", "active")
            ?.valueText
            ?.split(',')
            ?.asSequence()
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.toList() ?: listOf()
    }

    private fun getProfilesFromProperties(psiFile: PsiFile): List<String> {
        if (psiFile !is PropertiesFile) return listOf()

        return psiFile
            .properties
            .asSequence()
            .filter { "spring.profiles.active" == it.unescapedKey }
            .mapNotNull { it.unescapedValue }
            .flatMap { it.split(',') }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .toList()
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

    companion object {
        val fileMask = Regex("application(-.+)?\\.(properties|yml|yaml)")
    }

}