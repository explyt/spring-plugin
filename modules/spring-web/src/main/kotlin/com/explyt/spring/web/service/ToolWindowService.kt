/*
 * Copyright (c) 2025 Explyt Ltd
 * SPDX-License-Identifier: Apache-2.0
 */

package com.explyt.spring.web.service

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.SpringCoreUtil
import com.explyt.spring.web.util.SpringWebUtil.isEeWebProject
import com.explyt.spring.web.util.SpringWebUtil.isSpringWebProject
import com.intellij.ide.util.PropertiesComponent
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.application.ReadAction
import com.intellij.openapi.components.Service
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.startup.ProjectActivity
import com.intellij.openapi.wm.ToolWindowManager
import com.intellij.util.concurrency.AppExecutorUtil
import java.util.concurrent.Callable

@Service(Service.Level.PROJECT)
class ToolWindowService(private val project: Project) {

    private val propertiesComponent = PropertiesComponent.getInstance(project)

    init {
        project.messageBus.connect().subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                changeToolWindow(SpringCoreBundle.message("explyt.toolwindow.endpoints.title"))
            }
        })
    }

    fun changeToolWindow(toolWindowId: String) {
        ReadAction.nonBlocking(Callable {
            isSpringWebProject(project) || isEeWebProject(project) || SpringCoreUtil.isSpringBootProject(project)
        })
            .inSmartMode(project)
            .finishOnUiThread(ModalityState.nonModal()) { isVisible ->
                updateToolWindowVisibility(toolWindowId, isVisible)
                handleToolWindowNotification(toolWindowId, isVisible)
            }
            .submit(AppExecutorUtil.getAppScheduledExecutorService())
    }

    private fun updateToolWindowVisibility(toolWindowId: String, isVisible: Boolean) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(toolWindowId) ?: return
        toolWindow.isAvailable = isVisible
    }

    private fun handleToolWindowNotification(toolWindowId: String, shouldShow: Boolean) {
        val notificationExists = getToolWindowNotification(toolWindowId)
        if (!notificationExists && shouldShow) {
            setToolWindowNotificationIsShow(toolWindowId)
        }
    }

    private fun getToolWindowNotification(toolWindowId: String): Boolean {
        return propertiesComponent
            .getBoolean(propertyValue(toolWindowId), false)
    }

    private fun setToolWindowNotificationIsShow(toolWindowId: String) {
        propertiesComponent
            .setValue(propertyValue(toolWindowId), true, false)
    }

    class StartupActivity : ProjectActivity {
        override suspend fun execute(project: Project) {
            getInstance(project).changeToolWindow(SpringCoreBundle.message("explyt.toolwindow.endpoints.title"))
        }
    }

    companion object {
        private val LETTERS_PATTERN by lazy { "[^A-Za-z]" }

        fun getInstance(project: Project): ToolWindowService = project.service()

        private fun propertyValue(toolWindowId: String): String {
            return "Notification$toolWindowId".replace(LETTERS_PATTERN.toRegex(), "")
        }
    }

}
