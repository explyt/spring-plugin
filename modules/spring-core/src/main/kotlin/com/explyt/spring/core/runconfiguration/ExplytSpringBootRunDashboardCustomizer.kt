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

package com.explyt.spring.core.runconfiguration

import com.explyt.spring.core.SpringIcons
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootApplicationInfo
import com.explyt.spring.core.runconfiguration.lifecycle.SpringBootLifecycleManager
import com.intellij.execution.RunnerAndConfigurationSettings
import com.intellij.execution.dashboard.RunDashboardCustomizer
import com.intellij.execution.dashboard.RunDashboardRunConfigurationNode
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.icons.AllIcons
import com.intellij.ide.BrowserUtil
import com.intellij.ide.projectView.PresentationData
import com.intellij.ui.AnimatedIcon
import com.intellij.ui.SimpleTextAttributes

class ExplytSpringBootRunDashboardCustomizer : RunDashboardCustomizer() {

    override fun isApplicable(settings: RunnerAndConfigurationSettings, descriptor: RunContentDescriptor?): Boolean {
        return settings.configuration is SpringBootRunConfiguration
    }

    override fun updatePresentation(presentation: PresentationData, node: RunDashboardRunConfigurationNode): Boolean {
        val configuration = node.configurationSettings.configuration as? SpringBootRunConfiguration ?: return false
        val handler = node.descriptor?.processHandler ?: return false
        if (handler.isProcessTerminated) {
            node.putUserData(NODE_LINKS, null)
            return true
        }
        val lifecycleManager = SpringBootLifecycleManager.getInstance(node.project)

        val info = lifecycleManager.getInfo(handler)
        if (info == null) {
            node.putUserData(NODE_LINKS, null)
            return true
        }

        if (info.readyState.value == true) {
            presentation.setIcon(SpringIcons.Spring)
        } else {
            val session = lifecycleManager.getDebugSession(handler)
            if (session != null && session.isPaused) {
                presentation.setIcon(AllIcons.Process.Step_4)
            } else {
                presentation.setIcon(AnimatedIcon.Default.INSTANCE)
            }
        }

        val port = info.serverPort.value
        if (port == null || port <= 0) {
            node.putUserData(NODE_LINKS, null)
            return true
        }
        val link = ":$port"
        presentation.addText(link, SimpleTextAttributes.REGULAR_ATTRIBUTES)
        node.putUserData(NODE_LINKS, mapOf(link to ApplicationLinkListener(info, configuration)))

        return true
    }

    class ApplicationLinkListener(
        private val info: SpringBootApplicationInfo,
        val configuration: SpringBootRunConfiguration
    ) :
        Runnable {

        override fun run() {
            val applicationUrl = info.applicationUrl.value ?: return
            BrowserUtil.browse(applicationUrl)
        }
    }

}