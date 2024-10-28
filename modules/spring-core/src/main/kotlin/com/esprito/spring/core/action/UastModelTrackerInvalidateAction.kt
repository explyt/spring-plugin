package com.esprito.spring.core.action

import com.esprito.spring.core.SpringCoreBundle
import com.esprito.spring.core.SpringIcons
import com.esprito.spring.core.service.SpringSearchServiceFacade
import com.esprito.spring.core.tracker.ModificationTrackerManager
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
        getTemplatePresentation().text = SpringCoreBundle.message("esprito.spring.action.uast.model.cache.invalidate")
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
