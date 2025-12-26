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

package com.explyt.spring.core.action

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.service.PackageScanService
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.explyt.spring.core.util.ActionUtil
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager
import com.intellij.psi.search.searches.AnnotatedElementsSearch
import org.jetbrains.kotlin.idea.base.util.projectScope

class UastModelTrackerInvalidateAction : AnAction() {
    init {
        getTemplatePresentation().text = SpringCoreBundle.message("explyt.spring.action.uast.model.cache.invalidate")
        getTemplatePresentation().icon = SpringIcons.Spring
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return

        val project1 = getCurrentProject() ?: return
        val projectScope = project1.projectScope()
        val springBootAppAnnotations = PackageScanService.getInstance(project1).getSpringBootAppAnnotations()
        val mapNotNull = springBootAppAnnotations.mapNotNull { it.qualifiedName }
        println("!!!2 $mapNotNull")
        val map = springBootAppAnnotations.asSequence()
            .flatMap { AnnotatedElementsSearch.searchPsiClasses(it, projectScope) }
            .mapNotNull { it.qualifiedName }
            .toSet()
        println("!!!3 $map")

        invalidate(project)
    }

    private fun getCurrentProject(): Project? {
        val openProjects = ProjectManager.getInstance().openProjects
        return openProjects.firstOrNull { !it.isDefault }
    }

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val p = e.presentation
        if (project == null) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val virtualFiles = e.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY)
        if (virtualFiles == null || virtualFiles.size > 1) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val module = ModuleUtilCore.findModuleForFile(file, project)
        if (module == null) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        if (file.canonicalPath != project.basePath || file != project.guessProjectDir()) {
            ActionUtil.isEnabledAndVisible(e, false)
            return
        }
        val activeBeans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        p.isEnabledAndVisible = activeBeans.isNotEmpty()
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    companion object {
        fun invalidate(project: Project) {
            ModificationTrackerManager.getInstance(project).invalidateAll()
            PsiManager.getInstance(project).dropPsiCaches()
            DaemonCodeAnalyzer.getInstance(project).restart()
        }
    }
}
