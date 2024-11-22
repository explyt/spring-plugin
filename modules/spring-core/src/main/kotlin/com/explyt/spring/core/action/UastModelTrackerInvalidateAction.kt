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
import com.explyt.spring.core.service.SpringSearchServiceFacade
import com.explyt.spring.core.tracker.ModificationTrackerManager
import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
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
            p.isVisible = false
            return
        }
        val file = e.getData(CommonDataKeys.VIRTUAL_FILE) ?: return
        val module = ModuleUtilCore.findModuleForFile(file, project) ?: return
        val activeBeans = SpringSearchServiceFacade.getInstance(project).getAllActiveBeans(module)
        p.setVisible(activeBeans.isNotEmpty())
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
