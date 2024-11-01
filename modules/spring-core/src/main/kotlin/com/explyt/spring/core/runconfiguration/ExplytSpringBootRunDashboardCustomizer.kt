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