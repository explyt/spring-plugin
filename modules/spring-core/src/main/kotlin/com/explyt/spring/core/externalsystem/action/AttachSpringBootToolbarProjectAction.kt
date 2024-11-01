package com.explyt.spring.core.externalsystem.action

import com.explyt.spring.core.SpringCoreBundle.message
import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.runconfiguration.SpringBootRunConfiguration
import com.intellij.execution.RunManager
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.DumbAwareAction
import com.intellij.openapi.util.registry.Registry

class AttachSpringBootToolbarProjectAction : DumbAwareAction() {
    init {
        templatePresentation.text = message("explyt.external.project.action.link.text")
        templatePresentation.icon = SpringIcons.SpringExplorer
    }

    override fun getActionUpdateThread(): ActionUpdateThread {
        return ActionUpdateThread.BGT
    }

    override fun update(e: AnActionEvent) {
        val presentation = e.presentation
        val project = e.project ?: return
        presentation.isEnabledAndVisible = RunManager.getInstance(project).selectedConfiguration
            ?.configuration is SpringBootRunConfiguration && Registry.`is`("explyt.spring.native")
    }

    override fun actionPerformed(e: AnActionEvent) {
        AttachSpringBootProjectAction().actionPerformed(e)
    }
}