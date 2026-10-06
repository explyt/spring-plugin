/*
 * Copyright (c) 2026 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.profile

import com.explyt.spring.core.SpringProperties.SPRING_PROFILES_GROUP
import com.explyt.spring.core.runconfiguration.RunConfigurationUtil
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.lang.properties.psi.PropertiesFile
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FilenameIndex
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.yaml.YAMLUtil
import org.jetbrains.yaml.psi.YAMLDocument
import org.jetbrains.yaml.psi.YAMLFile
import org.jetbrains.yaml.psi.YAMLKeyValue
import org.jetbrains.yaml.psi.YAMLScalar
import org.jetbrains.yaml.psi.YAMLSequence

/**
 * Groups are read only from profile-independent `application.*` files and from YAML documents without
 * `spring.config.activate.on-profile`: Spring Boot cannot activate profiles from any other document.
 * Groups declared by several applications of the project are merged.
 */
class ProfileGroups(private val membersByGroup: Map<String, List<String>>) {

    fun expand(profiles: Collection<String>): Set<String> {
        val pending = ArrayDeque<String>()
        profiles.reversed().forEach { pending.addFirst(it) }
        val expanded = LinkedHashSet<String>()
        while (pending.isNotEmpty()) {
            val current = pending.removeFirst()
            if (expanded.add(current)) {
                membersByGroup[current].orEmpty().reversed().forEach { pending.addFirst(it) }
            }
        }
        return expanded
    }

    companion object {
        private const val GROUP_KEY_PREFIX = "$SPRING_PROFILES_GROUP."
        private const val ON_PROFILE = "spring.config.activate.on-profile"
        private val APPLICATION_CONFIG_FILE_NAMES =
            listOf("application.properties", "application.yaml", "application.yml")

        fun isProfileIndependentApplicationConfig(fileName: String): Boolean = fileName.startsWith("application.")

        fun of(project: Project): ProfileGroups {
            return CachedValuesManager.getManager(project).getCachedValue(project) {
                CachedValueProvider.Result(
                    runReadAction { read(project) },
                    ModificationTrackerManager.getInstance(project).getUastModelAndLibraryTracker()
                )
            }
        }

        private fun read(project: Project): ProfileGroups {
            val membersByGroup = LinkedHashMap<String, MutableList<String>>()
            applicationConfigFiles(project).forEach { file ->
                groupDeclarations(file).forEach { (key, values) ->
                    val group = key.removePrefix(GROUP_KEY_PREFIX).substringBefore('[')
                    val members = membersByGroup.getOrPut(group) { mutableListOf() }
                    values.flatMap { RunConfigurationUtil.stringToProfile(it) }.filterTo(members) { it !in members }
                }
            }
            return ProfileGroups(membersByGroup)
        }

        private fun applicationConfigFiles(project: Project): List<PsiFile> {
            val psiManager = PsiManager.getInstance(project)
            val scope = GlobalSearchScope.projectScope(project)
            return APPLICATION_CONFIG_FILE_NAMES
                .flatMap { FilenameIndex.getVirtualFilesByName(it, scope) }
                .mapNotNull { psiManager.findFile(it) }
        }

        private fun groupDeclarations(file: PsiFile): List<Pair<String, List<String>>> = when (file) {
            is PropertiesFile -> file.properties
                .filter { it.key?.startsWith(GROUP_KEY_PREFIX) == true }
                .map { it.key!! to listOfNotNull(it.value) }

            is YAMLFile -> file.documents.filterNot { activatesOnProfile(it) }.flatMap { yamlGroupDeclarations(it) }
            else -> emptyList()
        }

        private fun activatesOnProfile(document: YAMLDocument): Boolean =
            keyValues(document).any { YAMLUtil.getConfigFullName(it) == ON_PROFILE }

        private fun yamlGroupDeclarations(document: YAMLDocument): List<Pair<String, List<String>>> =
            keyValues(document).mapNotNull { keyValue ->
                val key = YAMLUtil.getConfigFullName(keyValue)
                if (!key.startsWith(GROUP_KEY_PREFIX)) return@mapNotNull null
                when (val value = keyValue.value) {
                    is YAMLSequence -> key to value.items.mapNotNull { (it.value as? YAMLScalar)?.textValue }
                    is YAMLScalar -> key to listOf(value.textValue)
                    else -> null
                }
            }

        private fun keyValues(document: YAMLDocument): List<YAMLKeyValue> =
            PsiTreeUtil.findChildrenOfType(document, YAMLKeyValue::class.java).toList()
    }
}
