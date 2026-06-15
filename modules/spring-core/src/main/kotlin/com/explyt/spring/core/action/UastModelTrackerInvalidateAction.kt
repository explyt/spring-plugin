/*
 * Copyright (c) 2024 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.core.action

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.SpringIcons
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
import com.intellij.openapi.project.guessProjectDir
import com.intellij.psi.PsiManager

class UastModelTrackerInvalidateAction : AnAction() {
    init {
        getTemplatePresentation().text = SpringCoreBundle.message("explyt.spring.action.uast.model.cache.invalidate")
        getTemplatePresentation().icon = SpringIcons.Spring
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        invalidate(project)
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
