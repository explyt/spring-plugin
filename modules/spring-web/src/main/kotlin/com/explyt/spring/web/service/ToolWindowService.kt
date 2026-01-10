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

package com.explyt.spring.web.service

import com.explyt.spring.core.SpringCoreBundle
import com.explyt.spring.core.util.SpringCoreUtil
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

    init {
        project.messageBus.connect().subscribe(ModuleRootListener.TOPIC, object : ModuleRootListener {
            override fun rootsChanged(event: ModuleRootEvent) {
                changeToolWindow(SpringCoreBundle.message("explyt.toolwindow.endpoints.title"))
            }
        })
    }

    fun changeToolWindow(toolWindowId: String) {
        ReadAction.nonBlocking(Callable { SpringCoreUtil.isSpringProject(project) })
            .inSmartMode(project)
            .finishOnUiThread(ModalityState.nonModal()) { isVisible ->
                updateToolWindowVisibility(toolWindowId, isVisible)
            }
            .submit(AppExecutorUtil.getAppScheduledExecutorService())
    }

    private fun updateToolWindowVisibility(toolWindowId: String, isVisible: Boolean) {
        val toolWindowManager = ToolWindowManager.getInstance(project)
        val toolWindow = toolWindowManager.getToolWindow(toolWindowId) ?: return
        toolWindow.isAvailable = isVisible
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
