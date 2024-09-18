package com.esprito.spring.web.tracker

import com.intellij.json.JsonLanguage
import com.intellij.json.json5.Json5Language
import com.intellij.lang.Language
import com.intellij.openapi.components.Service
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiManager
import org.jetbrains.yaml.YAMLLanguage

@Service(Service.Level.PROJECT)
class OpenApiLanguagesModificationTracker(val project: Project) : ModificationTracker {

    private var languageTrackers: List<ModificationTracker>

    init {
        val psiManager = PsiManager.getInstance(project)
        val languages = listOf<Language>(JsonLanguage.INSTANCE, Json5Language.INSTANCE, YAMLLanguage.INSTANCE)
        languageTrackers = languages.map { psiManager.modificationTracker.forLanguage(it) }
    }

    override fun getModificationCount(): Long {
        return languageTrackers.sumOf { it.modificationCount }
    }
}