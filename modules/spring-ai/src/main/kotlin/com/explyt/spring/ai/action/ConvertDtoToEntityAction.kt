/*
 * Copyright © 2025 Explyt Ltd
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

package com.explyt.spring.ai.action

import com.explyt.spring.ai.SpringAiBundle
import com.explyt.spring.ai.service.AiPluginService
import com.explyt.spring.ai.service.AiUtils
import com.explyt.spring.core.SpringCoreClasses
import com.explyt.spring.core.util.ActionUtil
import com.explyt.util.ExplytPsiUtil.isMetaAnnotatedBy
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.uast.UClass
import org.jetbrains.uast.UFile
import org.jetbrains.uast.toUElement

private val JPA_ANNOTATIONS = listOf("javax.persistence.Entity", "jakarta.persistence.Entity")
private const val MAX_FILES = 30

class ConvertDtoToEntityAction : AnAction(SpringAiBundle.message("explyt.spring.ai.action.dto.to.jpa")) {
    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > MAX_FILES) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val uClasses = virtualFiles
            .mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            .flatMap { it.classes }

        val enabled = uClasses.isNotEmpty() && uClasses.none { isPotentialSpringClass(it) }

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dtoPsiClasses = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            ?.flatMap { it.classes }
            ?.filter { !isPotentialSpringClass(it) } ?: return

        val dtoNames = dtoPsiClasses.map { it.javaPsi.name }
        val virtualFiles = dtoPsiClasses.mapNotNull { it.javaPsi.containingFile?.virtualFile }
        val jpaEntitySentence = AiUtils.getJpaEntitySentence(project)

        val prompt = SpringAiBundle.message("action.prompt.convert.dto", jpaEntitySentence, dtoNames)
        AiPluginService.getInstance(project).performPrompt(prompt, virtualFiles)
    }

    private fun isPotentialSpringClass(uClass: UClass): Boolean {
        if (uClass.javaPsi.isMetaAnnotatedBy(JPA_ANNOTATIONS)) return true
        if (uClass.javaPsi.isMetaAnnotatedBy(SpringCoreClasses.COMPONENT)) return true
        return uClass.uAnnotations.any { it.qualifiedName?.contains("spring", true) == true }
    }
}

class ConvertEntityToDtoAction : AnAction(SpringAiBundle.message("explyt.spring.ai.action.jpa.to.dto")) {
    override fun update(e: AnActionEvent) {
        val project: Project = e.project ?: return
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > MAX_FILES) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }

        val uClasses = virtualFiles
            .mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            .flatMap { it.classes }

        val enabled = uClasses.isNotEmpty() && uClasses.any { it.javaPsi.isMetaAnnotatedBy(JPA_ANNOTATIONS) }

        e.presentation.isEnabledAndVisible = enabled
    }

    override fun getActionUpdateThread(): ActionUpdateThread = ActionUpdateThread.BGT

    override fun actionPerformed(e: AnActionEvent) {
        val project: Project = e.project ?: return

        val dtoPsiClasses = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
            ?.mapNotNull { it.toPsiFile(project)?.toUElement() as? UFile }
            ?.flatMap { it.classes }
            ?.filter { it.javaPsi.isMetaAnnotatedBy(JPA_ANNOTATIONS) } ?: return

        val dtoNames = dtoPsiClasses.map { it.javaPsi.name }
        val virtualFiles = dtoPsiClasses.mapNotNull { it.javaPsi.containingFile?.virtualFile }

        val prompt = SpringAiBundle.message("action.prompt.convert.entity", dtoNames)
        AiPluginService.getInstance(project).performPrompt(prompt, virtualFiles)
    }
}