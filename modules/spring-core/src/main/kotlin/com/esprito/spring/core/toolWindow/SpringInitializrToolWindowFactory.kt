package com.esprito.spring.core.toolWindow

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.SimpleToolWindowPanel
import com.intellij.openapi.wm.ToolWindow
import com.intellij.openapi.wm.ToolWindowFactory
import com.intellij.ui.content.ContentFactory
import java.awt.Dimension
import javax.swing.JComponent
import javax.swing.JPanel

class SpringInitializrToolWindowFactory : ToolWindowFactory {
    override fun createToolWindowContent(project: Project, toolWindow: ToolWindow) {
        val springInitializrToolWindow = SpringInitializrToolWindow(project)
        val contentFactory = ContentFactory.getInstance()
        val content = contentFactory.createContent(springInitializrToolWindow.getContent(), "", false)
        toolWindow.contentManager.addContent(content)
    }

    class SpringInitializrToolWindow(project: Project) {
        private val contentToolWindow: JPanel = SimpleToolWindowPanel(true, true)

        init {
            this.contentToolWindow.preferredSize = Dimension(1080, 880)
        }

        fun getContent(): JComponent {
            return this.contentToolWindow
        }

    }
}